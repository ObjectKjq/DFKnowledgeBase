package com.df.knowledge.job;

import com.df.knowledge.service.doc.DocIngestService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 每十分钟扫描待处理文档，执行解析与索引入库。 */
@Component
public class DocParseJob {

    @Resource
    private DocIngestService docIngestService;

    /** 每小时第 0、10、20、30、40、50 分钟执行，未完成文档由后续扫描继续处理。 */
    @Scheduled(cron = "0 */1 * * * ?")
    public void execute() {
        docIngestService.dispatchPendingDocs();
    }
}
