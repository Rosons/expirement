package cn.byteo.springaidemo.constant;

import java.util.Arrays;
import java.util.List;

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file FileTypeConstant
 * @since 2026/4/16
 */
public class FileTypeConstant {

    /**
     * 知识库上传允许的扩展名（白名单）
     */
    public static final List<String> ALLOWED_FILE_EXTENSIONS = Arrays.asList(
            "pdf", "txt", "md", "log", "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    );

    // PDF 文档
    public static final String PDF = "pdf";

    // 纯文本文档
    public static final String TXT = "txt";
    public static final String MARKDOWN = "md";
    public static final String LOG = "log";

    // Microsoft Office 文档 - Word
    public static final String DOCX = "docx";
    public static final String DOC = "doc";

    // Microsoft Office 文档 - Excel
    public static final String XLSX = "xlsx";
    public static final String XLS = "xls";

    // Microsoft Office 文档 - PowerPoint
    public static final String PPTX = "pptx";
    public static final String PPT = "ppt";

    /**
     * 判断是否为 PDF 文档
     */
    public static boolean isPdf(String fileType) {
        return PDF.equalsIgnoreCase(fileType);
    }

    /**
     * 判断是否为纯文本文档
     */
    public static boolean isPlainText(String fileType) {
        return TXT.equalsIgnoreCase(fileType) || MARKDOWN.equalsIgnoreCase(fileType) || LOG.equalsIgnoreCase(fileType);
    }

    /**
     * 判断是否为 Office 文档
     */
    public static boolean isOfficeDocument(String fileType) {
        return DOCX.equalsIgnoreCase(fileType) ||
                DOC.equalsIgnoreCase(fileType) ||
                XLSX.equalsIgnoreCase(fileType) ||
                XLS.equalsIgnoreCase(fileType) ||
                PPTX.equalsIgnoreCase(fileType) ||
                PPT.equalsIgnoreCase(fileType);
    }

    private FileTypeConstant() {}
}
