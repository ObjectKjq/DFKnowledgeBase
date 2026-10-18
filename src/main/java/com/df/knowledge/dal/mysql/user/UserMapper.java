package com.df.knowledge.dal.mysql.user;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.knowledge.controller.user.vo.user.UserPageReqVO;
import com.df.knowledge.dal.dataobject.user.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    /**
     * 根据用户账号查询用户
     *
     * @param username 用户账号
     * @return 用户
     */
    default UserDO selectByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username));
    }

    /**
     * 分页查询用户
     *
     * @param reqVO 分页查询条件
     * @return 用户分页
     */
    default Page<UserDO> selectPage(UserPageReqVO reqVO) {
        return selectPage(new Page<>(reqVO.getPageNum(), reqVO.getPageSize()),
                new LambdaQueryWrapper<UserDO>()
                        .like(StrUtil.isNotBlank(reqVO.getUsername()), UserDO::getUsername, reqVO.getUsername())
                        .like(StrUtil.isNotBlank(reqVO.getNickname()), UserDO::getNickname, reqVO.getNickname())
                        .eq(reqVO.getStatus() != null, UserDO::getStatus, reqVO.getStatus())
                        .orderByDesc(UserDO::getId));
    }
}
