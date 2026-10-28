package com.df.knowledge.job;

import com.df.knowledge.service.doc.DocIngestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

/** 验证文档解析任务的扫描入口和十分钟执行周期。 */
@ExtendWith(MockitoExtension.class)
class DocParseJobTest {

    @Mock
    private DocIngestService docIngestService;

    @InjectMocks
    private DocParseJob job;

    @Test
    void testExecute_scansPendingDocsEveryTenMinutes() throws Exception {
        // 准备参数：使用任务实际声明的执行计划验证跨小时边界。
        Scheduled scheduled = DocParseJob.class.getMethod("execute").getAnnotation(Scheduled.class);
        CronExpression cron = CronExpression.parse(scheduled.cron());
        LocalDateTime first = cron.next(LocalDateTime.of(2026, 10, 28, 11, 59));

        // 调用
        job.execute();

        // 断言：任务入口触发扫描，相邻两次执行间隔为十分钟。
        verify(docIngestService).dispatchPendingDocs();
        assertEquals(LocalDateTime.of(2026, 10, 28, 12, 0), first);
        assertEquals(first.plusMinutes(10), cron.next(first));
    }
}
