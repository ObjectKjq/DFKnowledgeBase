package com.df.knowledge.service.file;

import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.file.FileContentDO;
import com.df.knowledge.dal.dataobject.file.FileDO;
import com.df.knowledge.dal.mysql.file.FileContentMapper;
import com.df.knowledge.dal.mysql.file.FileMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.file.FileConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link FileServiceImpl} 的单元测试
 */
@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileMapper fileMapper;

    @Mock
    private FileContentMapper fileContentMapper;

    @InjectMocks
    private FileServiceImpl fileService;

    @Test
    void testCreateFile_success() {
        // 准备参数：白名单内格式与匹配的 Content-Type
        byte[] content = new byte[]{1, 2, 3};
        // mock 插入回填自增主键
        doAnswer(invocation -> {
            FileDO file = invocation.getArgument(0);
            file.setId(5L);
            return 1;
        }).when(fileMapper).insert(any(FileDO.class));

        // 调用
        FileDO file = fileService.createFile(content, "期末复习笔记.pdf", "application/pdf");

        // 断言：元数据与字节分表落库
        assertEquals(5L, file.getId().longValue());
        assertEquals("期末复习笔记.pdf", file.getName());
        assertEquals("application/pdf", file.getType());
        assertEquals(3L, file.getSize().longValue());
        ArgumentCaptor<FileContentDO> captor = ArgumentCaptor.forClass(FileContentDO.class);
        verify(fileContentMapper).insert(captor.capture());
        assertEquals(5L, captor.getValue().getFileId().longValue());
        assertArrayEquals(content, captor.getValue().getContent());
    }

    @Test
    void testCreateFile_empty() {
        // 调用并断言：空文件被拦截
        ServiceException exception = assertThrows(ServiceException.class,
                () -> fileService.createFile(new byte[0], "空.txt", "text/plain"));
        assertEquals(ErrorCodeConstants.FILE_IS_EMPTY.getCode(), exception.getErrorCode().getCode());
        verifyNoInteractions(fileMapper, fileContentMapper);
    }

    @Test
    void testCreateFile_typeNotAllowed() {
        // 准备参数：扩展名与 Content-Type 任一不在白名单都拒绝（防伪装）
        byte[] content = new byte[]{1};

        // 调用并断言：白名单外扩展名
        ServiceException extensionException = assertThrows(ServiceException.class,
                () -> fileService.createFile(content, "病毒.exe", "application/pdf"));
        assertEquals(ErrorCodeConstants.FILE_TYPE_NOT_ALLOWED.getCode(), extensionException.getErrorCode().getCode());

        // 调用并断言：扩展名合法但 Content-Type 伪装
        ServiceException typeException = assertThrows(ServiceException.class,
                () -> fileService.createFile(content, "期末复习笔记.pdf", "text/html"));
        assertEquals(ErrorCodeConstants.FILE_TYPE_NOT_ALLOWED.getCode(), typeException.getErrorCode().getCode());
        verifyNoInteractions(fileMapper, fileContentMapper);
    }

    @Test
    void testCreateFile_tooLarge() {
        // 准备参数：超过 50 MB 上限的文件
        byte[] content = new byte[(int) FileConstants.MAX_FILE_SIZE + 1];

        // 调用并断言：超过大小上限被拦截
        ServiceException exception = assertThrows(ServiceException.class,
                () -> fileService.createFile(content, "大文件.pdf", "application/pdf"));
        assertEquals(ErrorCodeConstants.FILE_TOO_LARGE.getCode(), exception.getErrorCode().getCode());
        verifyNoInteractions(fileMapper, fileContentMapper);
    }

    @Test
    void testDelete_noValidation() {
        // 调用：删除不做引用/归属校验，文件不存在也静默成功（逻辑删除由持久层兜底）
        fileService.delete(5L);

        // 断言：元数据与字节两表同删
        verify(fileMapper).deleteById(5L);
        verify(fileContentMapper).deleteByFileId(5L);
    }

    @Test
    void testGetContent_notFound() {
        // 准备参数：文件内容不存在
        when(fileContentMapper.selectByFileId(5L)).thenReturn(null);

        // 调用并断言：返回 null，由调用方决定 404 或业务错误
        assertNull(fileService.getContent(5L));
    }
}
