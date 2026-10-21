package com.df.knowledge.service.doc;

import cn.hutool.core.util.StrUtil;
import com.df.knowledge.common.exception.ServiceException;
import com.df.knowledge.dal.dataobject.doc.DocChunkDO;
import com.df.knowledge.dal.dataobject.doc.DocDO;
import com.df.knowledge.dal.mysql.doc.DocChunkMapper;
import com.df.knowledge.dal.mysql.doc.DocMapper;
import com.df.knowledge.enums.ErrorCodeConstants;
import com.df.knowledge.enums.doc.DocConstants;
import com.df.knowledge.enums.doc.DocFormatEnum;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.TextRun;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档解析、切分与分块落库 Service 实现类
 */
@Service
@Validated
@Slf4j
public class DocParseServiceImpl implements DocParseService {

    private static final List<Pattern> SEPARATORS = List.of(
            Pattern.compile("\\n\\s*\\n"), Pattern.compile("\\R"),
            Pattern.compile("[。！？.!?]"), Pattern.compile("\\s"));

    @Resource
    private DocMapper docMapper;

    @Resource
    private DocChunkMapper docChunkMapper;

    @Resource
    private DocFileService docFileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void parse(Long docId) {
        // 文档行锁使同文档重试与彻底删除串行，避免清理后又写回孤立分块；不同文档互不加锁
        DocDO doc = docMapper.selectByIdForUpdate(docId);
        if (doc == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        DocFormatEnum format = Arrays.stream(DocFormatEnum.values())
                .filter(value -> value.getCode().equals(doc.getFormat())).findFirst()
                .orElseThrow(() -> new ServiceException(ErrorCodeConstants.DOC_FORMAT_NOT_SUPPORTED));
        List<DocChunkDO> chunks = new ArrayList<>();
        try (InputStream input = docFileService.open(doc.getFilePath())) {
            boolean paged = format == DocFormatEnum.PDF || format == DocFormatEnum.PPT;
            List<String> pages = readPages(input, format);
            int offset = 0;
            for (int i = 0; i < pages.size(); i++) {
                String text = pages.get(i);
                split(docId, text, paged ? i + 1 : null, offset, chunks);
                offset += text.length();
            }
        } catch (InvalidPasswordException | EncryptedDocumentException ex) {
            throw new ServiceException(ErrorCodeConstants.DOC_PARSE_FAILED, "文档解析失败：文档已加密或密码错误");
        } catch (IOException | RuntimeException ex) {
            log.warn("[parse][文档({})文本抽取失败]", docId, ex);
            throw new ServiceException(ErrorCodeConstants.DOC_PARSE_FAILED,
                    StrUtil.subPre("文档解析失败：" + StrUtil.blankToDefault(ex.getMessage(), "文档损坏或格式不符"), 500));
        }
        if (chunks.isEmpty()) {
            throw new ServiceException(ErrorCodeConstants.DOC_PARSE_FAILED, "文档解析失败：未提取到文本，请检查文档内容（暂不支持扫描件 OCR）");
        }
        // 先完成全部解析，再在同一事务中替换，重试及重启恢复不留下重复或部分分块
        docChunkMapper.deleteByDocId(docId);
        chunks.forEach(docChunkMapper::insert);
    }

    @Override
    public List<DocChunkDO> getDocChunks(Long docId) {
        return docChunkMapper.selectListByDocId(docId);
    }

    /**
     * 六格式使用统一的文本页列表，非分页格式只产生一个无页码的文本单元
     */
    private List<String> readPages(InputStream input, DocFormatEnum format) throws IOException {
        return switch (format) {
            case PDF -> readPdfPages(input);
            case PPT -> {
                try (SlideShow<?, ?> slides = SlideShowFactory.create(input)) {
                    yield readSlidePages(slides);
                }
            }
            case WORD, EXCEL -> List.of(new ApachePoiDocumentParser().parse(input).text());
            case TXT, MARKDOWN -> List.of(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        };
    }

    /**
     * PDF 按物理页抽取文本，空白页也保留位置
     */
    private List<String> readPdfPages(InputStream input) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(input.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setLineSeparator("\n");
            stripper.setPageEnd("\n");
            List<String> pages = new ArrayList<>();
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                pages.add(stripper.getText(pdf));
            }
            return pages;
        }
    }

    /**
     * 统一抽取 PPT/PPTX 的页面文本，包含表格与组合形状；不混入备注、批注与母版
     */
    private <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>>
    List<String> readSlidePages(SlideShow<S, P> slides) {
        SlideShowExtractor<S, P> extractor = new SlideShowExtractor<>(slides);
        extractor.setNotesByDefault(false);
        extractor.setCommentsByDefault(false);
        extractor.setMasterByDefault(false);
        return slides.getSlides().stream().map(extractor::getText).toList();
    }

    /**
     * 直接在原文区间递归寻找段落、行、句子、空白边界，最后退化为字符切分。
     * 不重组或去除空白，保证引用的字符偏移能够精确还原分块文本
     */
    private void split(Long docId, String text, Integer pageNumber, int pageOffset, List<DocChunkDO> chunks) {
        int start = 0;
        while (start < text.length()) {
            int end = start;
            for (int count = 0; count < DocConstants.CHUNK_SIZE && end < text.length(); count++) {
                end += Character.charCount(text.codePointAt(end));
            }
            if (end < text.length()) {
                end = findBoundary(text, text.offsetByCodePoints(start, DocConstants.CHUNK_OVERLAP + 1), end, 0);
            }
            String chunkText = text.substring(start, end);
            if (!chunkText.isBlank()) {
                chunks.add(new DocChunkDO().setDocId(docId).setChunkIndex(chunks.size())
                        .setText(chunkText).setStartOffset(pageOffset + start).setEndOffset(pageOffset + end)
                        .setPageNumber(pageNumber));
            }
            if (end == text.length()) {
                break;
            }
            start = text.offsetByCodePoints(end, -DocConstants.CHUNK_OVERLAP);
        }
    }

    /**
     * 大分隔符无法在当前区间内切分时，递归使用更小的分隔符
     */
    private int findBoundary(String text, int minEnd, int maxEnd, int level) {
        if (level == SEPARATORS.size()) {
            return maxEnd;
        }
        Matcher matcher = SEPARATORS.get(level).matcher(text).region(minEnd - 1, maxEnd);
        int boundary = -1;
        while (matcher.find()) {
            boundary = matcher.end();
        }
        return boundary >= minEnd ? boundary : findBoundary(text, minEnd, maxEnd, level + 1);
    }
}
