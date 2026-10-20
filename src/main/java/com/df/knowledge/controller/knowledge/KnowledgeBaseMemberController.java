package com.df.knowledge.controller.knowledge;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.df.knowledge.common.CommonResult;
import com.df.knowledge.controller.knowledge.vo.member.KnowledgeBaseMemberInviteReqVO;
import com.df.knowledge.controller.knowledge.vo.member.KnowledgeBaseMemberRespVO;
import com.df.knowledge.controller.knowledge.vo.member.KnowledgeBaseMemberUpdateReqVO;
import com.df.knowledge.dal.dataobject.knowledge.KnowledgeBaseMemberDO;
import com.df.knowledge.dal.dataobject.user.UserDO;
import com.df.knowledge.service.knowledge.KnowledgeBaseMemberService;
import com.df.knowledge.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.df.knowledge.common.CommonResult.success;

/**
 * 知识库成员 Controller
 *
 * 接口仅要求登录，成员管理权限由服务层判定（仅共享知识库所有者可邀请、改角色与移除成员）
 */
@Tag(name = "knowledgeBaseMemberController")
@RestController
@RequestMapping("/knowledge/base/member")
@Validated
public class KnowledgeBaseMemberController {

    @Resource
    private KnowledgeBaseMemberService knowledgeBaseMemberService;

    @Resource
    private UserService userService;

    @PostMapping("/invite")
    @Operation(summary = "邀请成员", description = "按用户账号邀请成员并分配成员角色，仅共享知识库所有者可操作")
    public CommonResult<Boolean> inviteMember(@Valid @RequestBody KnowledgeBaseMemberInviteReqVO reqVO) {
        knowledgeBaseMemberService.inviteMember(StpUtil.getLoginIdAsLong(), reqVO);
        return success(true);
    }

    @PutMapping("/update-role")
    @Operation(summary = "更改成员角色", description = "仅共享知识库所有者可操作")
    public CommonResult<Boolean> updateMemberRole(@Valid @RequestBody KnowledgeBaseMemberUpdateReqVO reqVO) {
        knowledgeBaseMemberService.updateMemberRole(StpUtil.getLoginIdAsLong(), reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "移除成员", description = "仅共享知识库所有者可操作")
    public CommonResult<Boolean> removeMember(
            @Parameter(name = "knowledgeBaseId", description = "知识库编号", required = true, example = "2")
            @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
            @Parameter(name = "id", description = "成员编号", required = true, example = "1")
            @RequestParam("id") Long id) {
        knowledgeBaseMemberService.removeMember(StpUtil.getLoginIdAsLong(), knowledgeBaseId, id);
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获取成员列表", description = "返回知识库的成员及其用户账号与昵称")
    @Parameter(name = "knowledgeBaseId", description = "知识库编号", required = true, example = "1")
    public CommonResult<List<KnowledgeBaseMemberRespVO>> getMemberList(@RequestParam("knowledgeBaseId") Long knowledgeBaseId) {
        List<KnowledgeBaseMemberDO> members = knowledgeBaseMemberService.getMemberList(StpUtil.getLoginIdAsLong(), knowledgeBaseId);
        return success(members.stream().map(this::convertMemberRespVO).toList());
    }

    /**
     * 转换成员 Response VO，并补齐成员的用户账号与昵称
     *
     * @param member 成员
     * @return 成员 Response VO
     */
    private KnowledgeBaseMemberRespVO convertMemberRespVO(KnowledgeBaseMemberDO member) {
        KnowledgeBaseMemberRespVO respVO = BeanUtil.copyProperties(member, KnowledgeBaseMemberRespVO.class);
        UserDO user = userService.getUser(member.getUserId());
        respVO.setUsername(user.getUsername());
        respVO.setNickname(user.getNickname());
        return respVO;
    }
}
