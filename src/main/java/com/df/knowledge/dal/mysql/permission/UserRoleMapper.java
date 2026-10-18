package com.df.knowledge.dal.mysql.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.knowledge.dal.dataobject.permission.UserRoleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户和角色关联 Mapper
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleDO> {

    /**
     * 查询用户的角色关联列表
     *
     * @param userId 用户编号
     * @return 角色关联列表
     */
    default List<UserRoleDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<UserRoleDO>().eq(UserRoleDO::getUserId, userId));
    }

    /**
     * 查询角色被哪些用户使用
     *
     * @param roleId 角色编号
     * @return 角色关联列表
     */
    default List<UserRoleDO> selectListByRoleId(Long roleId) {
        return selectList(new LambdaQueryWrapper<UserRoleDO>().eq(UserRoleDO::getRoleId, roleId));
    }

    /**
     * 删除用户的全部角色关联
     *
     * @param userId 用户编号
     */
    default void deleteByUserId(Long userId) {
        delete(new LambdaQueryWrapper<UserRoleDO>().eq(UserRoleDO::getUserId, userId));
    }
}
