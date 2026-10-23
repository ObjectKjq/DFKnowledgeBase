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
import com.df.knowledge.enums.doc.DocStatusEnum;
import com.df.knowledge.service.file.FileService;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import org.apache.poi.ss.usermodel.*;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import java.awt.image.BufferedImage;
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
    private FileService fileService;

    @Resource
    private DocOcrService docOcrService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void parse(Long docId) {
        // 文档行锁使同文档重试与彻底删除串行，避免清理后又写回孤立分块；不同文档互不加锁
        DocDO doc = docMapper.selectByIdForUpdate(docId);
        if (doc == null) {
            throw new ServiceException(ErrorCodeConstants.DOC_NOT_FOUND);
        }
        // 旧任务等待行锁期间可能已完成索引，不能再替换 ES 正在引用的分块主键
        if (!DocStatusEnum.PARSING.getCode().equals(doc.getStatus())) {
            return;
        }
        DocFormatEnum format = Arrays.stream(DocFormatEnum.values())
                .filter(value -> value.getCode().equals(doc.getFormat())).findFirst()
                .orElseThrow(() -> new ServiceException(ErrorCodeConstants.DOC_FORMAT_NOT_SUPPORTED));
        List<DocChunkDO> chunks = new ArrayList<>();
        try (InputStream input = openDocContent(doc)) {
            boolean paged = format == DocFormatEnum.PDF || format == DocFormatEnum.PPT;
            List<TextPage> pages = readPages(input, format);
            int offset = 0;
            for (int i = 0; i < pages.size(); i++) {
                TextPage page = pages.get(i);
                String text = page.text();
                int firstChunk = chunks.size();
                split(docId, text, paged ? i + 1 : null, offset, chunks);
                for (int index = firstChunk; index < chunks.size(); index++) {
                    chunks.get(index).setSheetName(page.sheetName()).setOcr(page.ocr());
                }
                offset += text.length();
            }
        } catch (InvalidPasswordException | EncryptedDocumentException ex) {
            throw new ServiceException(ErrorCodeConstants.DOC_PARSE_FAILED, "文档解析失败：文档已加密或密码错误");
        } catch (ServiceException ex) {
            // 业务错误（如文件缺失）原样上抛，保留专属错误码
            throw ex;
        } catch (IOException | RuntimeException ex) {
            log.warn("[parse][文档({})文本抽取失败]", docId, ex);
            throw new ServiceException(ErrorCodeConstants.DOC_PARSE_FAILED,
                    StrUtil.subPre("文档解析失败：" + StrUtil.blankToDefault(ex.getMessage(), "文档损坏或格式不符"), 500));
        }
        if (chunks.isEmpty()) {
            throw new ServiceException(ErrorCodeConstants.DOC_PARSE_FAILED, "文档解析失败：未提取到文本，请检查文档内容");
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
     * 打开文档内容：新文件读文件库（ADR-0009），存量磁盘文件过渡期直接读盘（双读，迁移完成后移除）
     *
     * @param doc 文档
     * @return 文档输入流
     * @throws IOException 文件无法读取
     */
    private InputStream openDocContent(DocDO doc) throws IOException {
        if (doc.getFileId() != null) {
            byte[] content = fileService.getContent(doc.getFileId());
            if (content == null) {
                throw new ServiceException(ErrorCodeConstants.FILE_NOT_FOUND);
            }
            return new ByteArrayInputStream(content);
        }
        return Files.newInputStream(Path.of(doc.getFilePath()));
    }

    /**
     * 六格式使用统一的文本单元；Excel 按工作表拆分，其他非分页格式只产生一个单元
     */
    private List<TextPage> readPages(InputStream input, DocFormatEnum format) throws IOException {
        return switch (format) {
            case PDF -> readPdfPages(input);
            case PPT -> {
                try (SlideShow<?, ?> slides = SlideShowFactory.create(input)) {
                    yield readSlidePages(slides).stream().map(text -> new TextPage(text, null, false)).toList();
                }
            }
            case WORD -> List.of(new TextPage(readWord(input), null, false));
            case EXCEL -> readWorkbook(input);
            case TXT, MARKDOWN -> List.of(new TextPage(new String(input.readAllBytes(), StandardCharsets.UTF_8), null, false));
        };
    }

    /**
     * PDF 按物理页抽取文本，空白页也保留位置
     */
    private List<TextPage> readPdfPages(InputStream input) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(input.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper() {
                private org.apache.pdfbox.text.TextPosition previous;
                @Override
                protected void writeString(String text, List<org.apache.pdfbox.text.TextPosition> positions) throws IOException {
                    if (!positions.isEmpty()) {
                        var first = positions.getFirst();
                        if (previous != null && Math.abs(first.getYDirAdj() - previous.getYDirAdj()) < 2
                                && first.getXDirAdj() - previous.getXDirAdj() - previous.getWidthDirAdj()
                                > Math.max(8, previous.getWidthOfSpace() * 2)) {
                            super.writeString("\t");
                        }
                        previous = positions.getLast();
                    }
                    super.writeString(text, positions);
                }
                @Override
                protected void writeLineSeparator() throws IOException {
                    previous = null;
                    super.writeLineSeparator();
                }
            };
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");
            stripper.setPageEnd("\n");
            List<TextPage> pages = new ArrayList<>();
            PDFRenderer renderer = new PDFRenderer(pdf);
            for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                try {
                    String text = stripper.getText(pdf);
                    boolean ocr = false;
                    if (text.isBlank()) {
                        BufferedImage image = renderer.renderImageWithDPI(page - 1, 200, ImageType.RGB);
                        try {
                            if (hasInk(image)) {
                                text = docOcrService.recognize(image);
                                ocr = true;
                                if (text == null || text.isBlank()) {
                                    throw new IOException("OCR 未识别到文本，请检查扫描质量");
                                }
                            }
                        } finally {
                            image.flush();
                        }
                    }
                    pages.add(new TextPage(formatAlignedTables(ocr ? text.replaceAll(" {3,}", "\t") : text), null, ocr));
                } catch (Exception ex) {
                    throw new ServiceException(ErrorCodeConstants.DOC_PARSE_FAILED,
                            "文档解析失败：第 " + page + " 页：" + StrUtil.blankToDefault(ex.getMessage(), "页面抽取失败"));
                }
            }
            return pages;
        }
    }

    /** PDF 中有明显横向间隔的连续多列表格，按表头和列次序保留关系。 */
    private String formatAlignedTables(String text) {
        if (!text.contains("\t")) return text;
        StringBuilder result = new StringBuilder();
        List<List<String>> rows = new ArrayList<>();
        for (String line : text.split("\\R", -1)) {
            if (line.contains("\t")) {
                rows.add(Arrays.stream(line.split("\t", -1)).map(String::strip).toList());
            } else {
                if (!rows.isEmpty()) {
                    if (rows.size() > 1) {
                        result.append(formatTable(rows));
                    } else {
                        result.append(String.join("\t", rows.getFirst())).append('\n');
                    }
                    rows.clear();
                }
                result.append(line).append('\n');
            }
        }
        if (!rows.isEmpty()) result.append(formatTable(rows));
        return result.toString();
    }

    /** 空白页保留物理位置，不把无内容页当作 OCR 失败。 */
    private boolean hasInk(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                if ((rgb & 0xff) < 240 || ((rgb >> 8) & 0xff) < 240 || ((rgb >> 16) & 0xff) < 240) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Word 段落与表格按文档顺序抽取，每个表格数据行重复表头。 */
    private String readWord(InputStream input) throws IOException {
        byte[] bytes = input.readAllBytes();
        StringBuilder text = new StringBuilder();
        if (org.apache.poi.poifs.filesystem.FileMagic.valueOf(bytes) == org.apache.poi.poifs.filesystem.FileMagic.OLE2) {
            try (var word = new org.apache.poi.hwpf.HWPFDocument(new ByteArrayInputStream(bytes))) {
                var range = word.getRange();
                for (int index = 0; index < range.numParagraphs();) {
                    var paragraph = range.getParagraph(index);
                    if (paragraph.isInTable()) {
                        var table = range.getTable(paragraph);
                        List<List<String>> rows = new ArrayList<>();
                        for (int row = 0; row < table.numRows(); row++) {
                            List<String> cells = new ArrayList<>();
                            var tableRow = table.getRow(row);
                            for (int column = 0; column < tableRow.numCells(); column++) {
                                cells.add(tableRow.getCell(column).text().replace("\u0007", "").strip());
                            }
                            rows.add(cells);
                        }
                        text.append(formatTable(rows));
                        index += table.numParagraphs();
                    } else {
                        text.append(paragraph.text());
                        index++;
                    }
                }
            }
        } else {
            try (var word = new org.apache.poi.xwpf.usermodel.XWPFDocument(new ByteArrayInputStream(bytes))) {
                for (var element : word.getBodyElements()) {
                    if (element instanceof org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph) {
                        text.append(paragraph.getText()).append('\n');
                    } else if (element instanceof org.apache.poi.xwpf.usermodel.XWPFTable table) {
                        text.append(formatTable(table.getRows().stream().map(row -> row.getTableCells().stream()
                                .map(org.apache.poi.xwpf.usermodel.XWPFTableCell::getText).toList()).toList()));
                    }
                }
            }
        }
        return text.toString();
    }

    /** 采用显式行号、列号及表头，保持不等长行的单元格归属。 */
    private String formatTable(List<List<String>> rows) {
        if (rows.isEmpty()) return "";
        StringBuilder text = new StringBuilder();
        List<String> headers = rows.getFirst();
        for (int row = 0; row < rows.size(); row++) {
            text.append("第 ").append(row + 1).append(" 行：");
            List<String> cells = rows.get(row);
            for (int column = 0; column < cells.size(); column++) {
                if (column > 0) text.append(" | ");
                String header = column < headers.size() ? headers.get(column) : "无表头";
                text.append("列 ").append(column + 1).append(" ").append(header).append("=").append(cells.get(column));
            }
            text.append('\n');
        }
        return text.toString();
    }

    /** 工作表单独切分，每个数据行重复表头及列坐标，空单元格不左移。 */
    private List<TextPage> readWorkbook(InputStream input) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<TextPage> pages = new ArrayList<>();
            for (Sheet sheet : workbook) {
                Row header = sheet.getRow(sheet.getFirstRowNum());
                if (header == null) {
                    continue;
                }
                int columns = header.getLastCellNum();
                for (Row row : sheet) {
                    columns = Math.max(columns, row.getLastCellNum());
                }
                List<String> headers = new ArrayList<>();
                for (int column = 0; column < columns; column++) {
                    headers.add(formatter.formatCellValue(header.getCell(column), evaluator));
                }
                StringBuilder text = new StringBuilder("工作表【" + sheet.getSheetName() + "】\n");
                for (Row row : sheet) {
                    text.append("第 ").append(row.getRowNum() + 1).append(" 行：");
                    for (int column = 0; column < columns; column++) {
                        if (column > 0) text.append(" | ");
                        text.append(org.apache.poi.ss.util.CellReference.convertNumToColString(column))
                                .append(" ").append(StrUtil.blankToDefault(headers.get(column), "无表头"))
                                .append("=").append(formatter.formatCellValue(row.getCell(column), evaluator));
                    }
                    text.append('\n');
                }
                pages.add(new TextPage(text.toString(), sheet.getSheetName(), false));
            }
            return pages;
        }
    }

    /** 抽取单元及原文定位属性。 */
    private record TextPage(String text, String sheetName, boolean ocr) { }

    /**
     * 统一抽取 PPT/PPTX 的页面文本，包含表格与组合形状；不混入备注、批注与母版
     */
    private <S extends Shape<S, P>, P extends TextParagraph<S, P, ? extends TextRun>>
    List<String> readSlidePages(SlideShow<S, P> slides) {
        SlideShowExtractor<S, P> extractor = new SlideShowExtractor<>(slides);
        extractor.setNotesByDefault(false);
        extractor.setCommentsByDefault(false);
        extractor.setMasterByDefault(false);
        return slides.getSlides().stream().map(slide -> {
            try {
                StringBuilder text = new StringBuilder(extractor.getText(slide));
                // 提取器提供正文；额外显式记录表格行列，避免表头与数值失去对应。
                for (S shape : slide.getShapes()) {
                    if (shape instanceof org.apache.poi.sl.usermodel.TableShape<?, ?> table) {
                        List<List<String>> rows = new ArrayList<>();
                        for (int row = 0; row < table.getNumberOfRows(); row++) {
                            List<String> cells = new ArrayList<>();
                            for (int column = 0; column < table.getNumberOfColumns(); column++) {
                                var cell = table.getCell(row, column);
                                cells.add(cell == null ? "" : cell.getText());
                            }
                            rows.add(cells);
                        }
                        text.append('\n').append(formatTable(rows));
                    }
                }
                return text.toString();
            } catch (RuntimeException ex) {
                throw new ServiceException(ErrorCodeConstants.DOC_PARSE_FAILED,
                        "文档解析失败：第 " + slide.getSlideNumber() + " 页："
                                + StrUtil.blankToDefault(ex.getMessage(), "幻灯片抽取失败"));
            }
        }).toList();
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
