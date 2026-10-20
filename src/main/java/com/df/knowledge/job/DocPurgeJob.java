package com.df.knowledge.job;

import com.df.knowledge.service.doc.DocService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 文档超期彻底删除 Job
 *
 * 软删除超过 30 天保留期的文档自动彻底删除（PRD §8.2 文档生命周期）
 */
@Component
@Slf4j
public class DocPurgeJob {

    @Resource
    private DocService docService;

    /**
     * 每天凌晨 3 点清理超期的软删除文档
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void execute() {
        log.info("[execute][开始清理超期软删除文档]");
        docService.purgeExpiredDeletedDocs();
    }
}
