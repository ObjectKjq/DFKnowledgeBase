package com.df.knowledge.service.doc;

import java.awt.image.BufferedImage;
import java.io.IOException;

/** 扫描页 OCR 的外部进程边界。 */
public interface DocOcrService {
    /**
     * 识别单页图像。
     * @param image 页面图像
     * @return 识别文本
     * @throws IOException 进程不可用、超时或识别失败
     */
    String recognize(BufferedImage image) throws IOException;
}
