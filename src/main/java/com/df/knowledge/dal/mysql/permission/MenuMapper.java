package com.df.knowledge.dal.mysql.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.knowledge.dal.dataobject.permission.MenuDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * 菜单 Mapper
 */
@Mapper
public interface MenuMapper extends BaseMapper<MenuDO> {

    /**
     * 查询全部菜单（按 sort 升序）
     *
     * @return 菜单列表
     */
    default List<MenuDO> selectList() {
        return selectList(new LambdaQueryWrapper<MenuDO>().orderByAsc(MenuDO::getSort));
    }

    /**
     * 查询指定编号的菜单列表
     *
     * @param ids 菜单编号集合
     * @return 菜单列表
     */
    default List<MenuDO> selectListByIds(Collection<Long> ids) {
        return selectList(new LambdaQueryWrapper<MenuDO>().in(MenuDO::getId, ids));
    }

    /**
     * 统计某父菜单下的子菜单数量
     *
     * @param parentId 父菜单编号
     * @return 子菜单数量
     */
    default Long selectCountByParentId(Long parentId) {
        return selectCount(new LambdaQueryWrapper<MenuDO>().eq(MenuDO::getParentId, parentId));
    }
}
