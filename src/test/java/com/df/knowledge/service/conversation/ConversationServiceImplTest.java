package com.df.knowledge.service.conversation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.controller.conversation.vo.ConversationCreateReqVO;
import com.df.knowledge.controller.conversation.vo.MessageCreateReqVO;
import com.df.knowledge.dal.dataobject.conversation.ConversationDO;
import com.df.knowledge.dal.dataobject.conversation.MessageDO;
import com.df.knowledge.dal.mysql.conversation.ConversationMapper;
import com.df.knowledge.dal.mysql.conversation.MessageMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.service.knowledge.KnowledgeBasePermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ConversationServiceImpl} 的单元测试
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private ConversationMapper conversationMapper;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private KnowledgeBasePermissionService knowledgeBasePermissionService;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    @Test
    void testCreateConversation_success() {
        // 准备参数：范围重复时保留首次出现顺序并校验内容权限
        ConversationCreateReqVO reqVO = new ConversationCreateReqVO();
        reqVO.setTitle("如何让知识检索更可信？");
        reqVO.setKnowledgeBaseIds(List.of(3L, 1L, 3L));
        doAnswer(invocation -> {
            ConversationDO conversation = invocation.getArgument(0);
            conversation.setId(8L);
            return 1;
        }).when(conversationMapper).insert(any(ConversationDO.class));

        // 调用
        Long id = conversationService.createConversation(10L, reqVO);

        // 断言：会话只绑定当前用户，并记录发起时的检索范围
        assertEquals(8L, id.longValue());
        verify(knowledgeBasePermissionService).checkViewPermission(10L, 3L);
        verify(knowledgeBasePermissionService).checkViewPermission(10L, 1L);
        ArgumentCaptor<ConversationDO> captor = ArgumentCaptor.forClass(ConversationDO.class);
        verify(conversationMapper).insert(captor.capture());
        assertEquals(10L, captor.getValue().getUserId().longValue());
        assertEquals("如何让知识检索更可信？", captor.getValue().getTitle());
        assertEquals(List.of(3L, 1L), captor.getValue().getKnowledgeBaseIds());
    }

    @Test
    void testGetConversationList_onlyCurrentUser() {
        // 准备参数
        ConversationDO conversation = buildConversation(1L, 10L);
        when(conversationMapper.selectListByUserId(10L)).thenReturn(List.of(conversation));

        // 调用
        List<ConversationDO> result = conversationService.getConversationList(10L);

        // 断言：查询条件固定为当前用户
        assertEquals(List.of(conversation), result);
        verify(conversationMapper).selectListByUserId(10L);
    }

    @Test
    void testGetConversation_otherUser_notFound() {
        // 准备参数：目标会话不属于当前用户
        when(conversationMapper.selectByIdAndUserId(1L, 20L)).thenReturn(null);

        // 调用并断言：不泄露他人会话是否存在
        ServiceException exception = assertThrows(ServiceException.class,
                () -> conversationService.getConversation(20L, 1L));
        assertEquals(ErrorCodeConstants.CONVERSATION_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void testDeleteConversation_success() {
        // 准备参数
        when(conversationMapper.selectByIdAndUserId(1L, 10L)).thenReturn(buildConversation(1L, 10L));

        // 调用
        conversationService.deleteConversation(10L, 1L);

        // 断言：消息与会话在同一业务操作中删除
        verify(messageMapper).deleteByConversationId(1L);
        verify(conversationMapper).deleteById(1L);
    }

    @Test
    void testDeleteConversation_otherUser_notFound() {
        // 准备参数：目标会话不属于当前用户
        when(conversationMapper.selectByIdAndUserId(1L, 20L)).thenReturn(null);

        // 调用并断言：拒绝删除，且不触碰消息
        ServiceException exception = assertThrows(ServiceException.class,
                () -> conversationService.deleteConversation(20L, 1L));
        assertEquals(ErrorCodeConstants.CONVERSATION_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
        verify(messageMapper, never()).deleteByConversationId(any());
        verify(conversationMapper, never()).deleteById(any());
    }

    @Test
    void testCreateMessage_success() {
        // 准备参数
        when(conversationMapper.selectByIdAndUserId(1L, 10L)).thenReturn(buildConversation(1L, 10L));
        MessageCreateReqVO reqVO = new MessageCreateReqVO();
        reqVO.setConversationId(1L);
        reqVO.setType("user");
        reqVO.setContent(OBJECT_MAPPER.createObjectNode().put("text", "知识检索如何工作"));
        doAnswer(invocation -> {
            MessageDO message = invocation.getArgument(0);
            message.setId(6L);
            return 1;
        }).when(messageMapper).insert(any(MessageDO.class));

        // 调用
        Long id = conversationService.createMessage(10L, reqVO);

        // 断言
        assertEquals(6L, id.longValue());
        ArgumentCaptor<MessageDO> captor = ArgumentCaptor.forClass(MessageDO.class);
        verify(messageMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getConversationId().longValue());
        assertEquals("user", captor.getValue().getType());
        assertEquals("知识检索如何工作", captor.getValue().getContent().get("text").asText());
    }

    @Test
    void testCreateMessage_otherUser_notFound() {
        // 准备参数
        when(conversationMapper.selectByIdAndUserId(1L, 20L)).thenReturn(null);
        MessageCreateReqVO reqVO = new MessageCreateReqVO();
        reqVO.setConversationId(1L);
        reqVO.setType("user");
        reqVO.setContent(OBJECT_MAPPER.createObjectNode().put("text", "测试"));

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> conversationService.createMessage(20L, reqVO));
        assertEquals(ErrorCodeConstants.CONVERSATION_NOT_FOUND.getCode(), exception.getErrorCode().getCode());
        verify(messageMapper, never()).insert(any(MessageDO.class));
    }

    @Test
    void testGetMessageList_orderByCreateTimeAndId() {
        // 准备参数：Mapper 返回顺序不稳定，Service 仍保证时间正序与同秒编号正序
        when(conversationMapper.selectByIdAndUserId(1L, 10L)).thenReturn(buildConversation(1L, 10L));
        LocalDateTime firstTime = LocalDateTime.of(2026, 10, 21, 10, 0);
        LocalDateTime secondTime = firstTime.plusSeconds(1);
        MessageDO first = buildMessage(1L, firstTime);
        MessageDO second = buildMessage(2L, secondTime);
        MessageDO third = buildMessage(3L, secondTime);
        when(messageMapper.selectListByConversationId(1L)).thenReturn(List.of(third, second, first));

        // 调用
        List<MessageDO> result = conversationService.getMessageList(10L, 1L);

        // 断言
        assertEquals(List.of(first, second, third), result);
    }

    private static ConversationDO buildConversation(Long id, Long userId) {
        ConversationDO conversation = new ConversationDO();
        conversation.setId(id);
        conversation.setUserId(userId);
        conversation.setTitle("会话");
        conversation.setKnowledgeBaseIds(List.of());
        return conversation;
    }

    private static MessageDO buildMessage(Long id, LocalDateTime createTime) {
        MessageDO message = new MessageDO();
        message.setId(id);
        message.setConversationId(1L);
        message.setType("note");
        message.setContent(JsonNodeFactoryHolder.EMPTY_OBJECT);
        message.setCreateTime(createTime);
        return message;
    }

    /**
     * 复用不可变空 JSON 节点，避免测试数据构造重复
     */
    private static final class JsonNodeFactoryHolder {
        private static final JsonNode EMPTY_OBJECT = OBJECT_MAPPER.createObjectNode();

        private JsonNodeFactoryHolder() {
        }
    }
}
