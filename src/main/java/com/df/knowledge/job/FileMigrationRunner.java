package com.df.knowledge.job;

import cn.hutool.core.io.FileUtil;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import com.df.knowledge.dal.dataobject.file.FileDO;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.enums.file.FileConstants;
import com.df.knowledge.service.file.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 存量文件迁移 Runner（一次性过渡工具，ADR-0009）
 *
 * 服务启动时把仍指向磁盘的存量文档读入文件库并改写引用（file_id / file_url，清空 file_path）。
 * 早于入库任务恢复（CommandLineRunner 先于 ApplicationReadyEvent）执行，迁移完成后即可移除双读逻辑与 file_path 列
 */
@Component
@Slf4j
public class FileMigrationRunner implements CommandLineRunner {

    @Resource
    private DocMapper docMapper;

    @Resource
    private FileService fileService;

    @Override
    public void run(String... args) {
        // 1. 只处理仍指向磁盘的存量文档
        List<DocDO> docs = docMapper.selectListByLegacyFile();
        if (docs.isEmpty()) {
            return;
        }
        // 2. 逐条迁移入库并改写引用，单条失败不阻塞其余文档
        int migrated = 0;
        for (DocDO doc : docs) {
            if (migrateDoc(doc)) {
                migrated++;
            }
        }
        log.info("[run][存量文件迁移完成({}/{}个)]", migrated, docs.size());
    }

    /**
     * 迁移单个存量文档的文件：读磁盘文件入库后改写引用并清空过渡路径
     *
     * @param doc 存量文档
     * @return 是否迁移成功
     */
    private boolean migrateDoc(DocDO doc) {
        // 1. 磁盘文件缺失或扩展名不在白名单时保留 file_path，交由日志排查与双读兜底
        List<String> allowedTypes = FileConstants.ALLOWED_CONTENT_TYPES
                .get(FileUtil.extName(doc.getFileName()).toLowerCase());
        if (allowedTypes == null) {
            log.warn("[migrateDoc][文档({})文件名({})扩展名不在白名单，跳过迁移]", doc.getId(), doc.getFileName());
            return false;
        }
        // 2. 文件内容入库（内容类型取白名单首项规范类型），随后改写引用
        try {
            byte[] content = Files.readAllBytes(Path.of(doc.getFilePath()));
            FileDO file = fileService.createFile(content, doc.getFileName(), allowedTypes.get(0));
            docMapper.updateFileRef(doc.getId(), file.getId(), fileService.buildDownloadUrl(file.getId()));
            return true;
        } catch (IOException ex) {
            log.warn("[migrateDoc][文档({})存量文件({})读取失败，保留 file_path 供双读]", doc.getId(), doc.getFilePath(), ex);
            return false;
        }
    }
}
