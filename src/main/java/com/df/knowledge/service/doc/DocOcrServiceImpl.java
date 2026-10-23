package com.df.knowledge.service.doc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import lombok.extern.slf4j.Slf4j;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** 通过 Tesseract 标准命令行执行本地 OCR，不将文档内容发送到远程服务。 */
@Service
@Validated
@Slf4j
public class DocOcrServiceImpl implements DocOcrService {
    @Value("${knowledge.ocr.binary:tesseract}")
    private String binary = "tesseract";
    @Value("${knowledge.ocr.languages:chi_sim+eng}")
    private String languages = "chi_sim+eng";
    @Value("${knowledge.ocr.timeout-seconds:120}")
    private long timeoutSeconds = 120;

    @Override
    public String recognize(BufferedImage image) throws IOException {
        Path directory;
        try {
            directory = Files.createTempDirectory("knowledge-ocr-");
        } catch (IOException ex) {
            throw new IOException("OCR 临时目录不可写，请检查服务账户权限");
        }
        Path input = directory.resolve("page.png");
        Path output = directory.resolve("result");
        Path error = directory.resolve("error.log");
        Process process = null;
        try {
            ImageIO.write(image, "png", input.toFile());
            try {
                process = new ProcessBuilder(binary, input.toString(), output.toString(), "-l", languages, "--psm", "3", "-c", "preserve_interword_spaces=1")
                        .redirectError(error.toFile()).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            } catch (IOException ex) {
                throw new IOException("OCR 引擎无法启动，请检查 Tesseract 安装及 binary 配置");
            }
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new IOException("OCR 识别超时");
            }
            if (process.exitValue() != 0) {
                throw new IOException("OCR 引擎失败（退出码 " + process.exitValue() + "），请检查语言包或扫描内容");
            }
            return Files.readString(directory.resolve("result.txt"), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            // 文件系统异常可能包含绝对路径，只向调用者保留可操作的安全原因。
            if (ex.getMessage() != null && ex.getMessage().startsWith("OCR ")) {
                throw ex;
            }
            throw new IOException("OCR 临时文件读写失败，请检查服务账户权限");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("OCR 识别已中断");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                try {
                    process.waitFor(5, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            // 清理失败不能覆盖最初的识别错误；日志不包含正文或临时路径。
            for (Path path : java.util.List.of(input, directory.resolve("result.txt"), error, directory)) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    log.warn("[recognize][OCR 临时文件清理失败]");
                }
            }
        }
    }
}
