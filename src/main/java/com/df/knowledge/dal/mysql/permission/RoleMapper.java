package com.df.knowledge.dal.mysql.permission;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.knowledge.common.CommonStatusEnum;
import com.df.knowledge.controller.permission.vo.role.RolePageReqVO;
import com.df.knowledge.dal.dataobject.permission.RoleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 角色 Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<RoleDO> {

    /**
     * 根据角色权限标识查询角色
     *
     * @param code 角色权限标识
     * @return 角色
     */
    default RoleDO selectByCode(String code) {
        return selectOne(new LambdaQueryWrapper<RoleDO>().eq(RoleDO::getCode, code));
    }

    /**
     * 根据角色名称查询角色
     *
     * @param name 角色名称
     * @return 角色
     */
    default RoleDO selectByName(String name) {
        return selectOne(new LambdaQueryWrapper<RoleDO>().eq(RoleDO::getName, name));
    }

    /**
     * 查询指定编号的角色列表
     *
     * @param ids 角色编号集合
     * @return 角色列表
     */
    default List<RoleDO> selectListByIds(Collection<Long> ids) {
        return selectList(new LambdaQueryWrapper<RoleDO>().in(RoleDO::getId, ids));
    }

    /**
     * 查询开启状态的角色列表
     *
     * @return 角色列表（按编号倒序）
     */
    default List<RoleDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getStatus, CommonStatusEnum.ENABLE.getCode())
                .orderByDesc(RoleDO::getId));
    }

    /**
     * 分页查询角色
     *
     * @param reqVO 分页查询条件
     * @return 角色分页
     */
    default Page<RoleDO> selectPage(RolePageReqVO reqVO) {
        return selectPage(new Page<>(reqVO.getPageNum(), reqVO.getPageSize()),
                new LambdaQueryWrapper<RoleDO>()
                        .like(StrUtil.isNotBlank(reqVO.getName()), RoleDO::getName, reqVO.getName())
                        .like(StrUtil.isNotBlank(reqVO.getCode()), RoleDO::getCode, reqVO.getCode())
                        .eq(reqVO.getStatus() != null, RoleDO::getStatus, reqVO.getStatus())
                        .orderByDesc(RoleDO::getId));
    }
}
