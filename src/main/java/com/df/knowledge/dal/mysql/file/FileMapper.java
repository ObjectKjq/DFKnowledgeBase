package com.df.knowledge.dal.mysql.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.knowledge.dal.dataobject.file.FileDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件 Mapper
 */
@Mapper
public interface FileMapper extends BaseMapper<FileDO> {
    /** 锁定文件，串行化关联与回收。
     * @param id 文件编号
     * @return 文件元数据
     */
    default FileDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapper<FileDO>()
                .eq(FileDO::getId, id).last("FOR UPDATE"));
    }

    /** 物理清理元数据。
     * @param id 文件编号
     * @return 清理条数
     */
    @Delete("DELETE FROM system_file WHERE id = #{id}")
    int purgeById(Long id);
}
