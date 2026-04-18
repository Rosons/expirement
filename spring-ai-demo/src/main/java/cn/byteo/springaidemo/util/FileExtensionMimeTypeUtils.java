package cn.byteo.springaidemo.util;

import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 根据文件后缀（扩展名）推断 MIME 类型。
 * <p>
 * 优先查内置映射，其次使用 {@link URLConnection#guessContentTypeFromName(String)}，
 * 仍无法识别时返回 {@value #DEFAULT_MIME_TYPE}。
 * </p>
 */
public final class FileExtensionMimeTypeUtils {

    /** 未知或空后缀时的默认类型 */
    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private static final Map<String, String> EXTENSION_TO_MIME;

    static {
        Map<String, String> m = new HashMap<>();
        // 图片
        put(m, "jpg", "image/jpeg");
        put(m, "jpeg", "image/jpeg");
        put(m, "png", "image/png");
        put(m, "gif", "image/gif");
        put(m, "webp", "image/webp");
        put(m, "bmp", "image/bmp");
        put(m, "svg", "image/svg+xml");
        put(m, "ico", "image/x-icon");
        put(m, "tif", "image/tiff");
        put(m, "tiff", "image/tiff");
        put(m, "heic", "image/heic");
        put(m, "avif", "image/avif");
        // 音频
        put(m, "mp3", "audio/mp3");
        put(m, "wav", "audio/wav");
        put(m, "ogg", "audio/ogg");
        put(m, "oga", "audio/ogg");
        put(m, "m4a", "audio/mp4");
        put(m, "aac", "audio/aac");
        put(m, "flac", "audio/flac");
        put(m, "opus", "audio/opus");
        // 视频
        put(m, "mp4", "video/mp4");
        put(m, "webm", "video/webm");
        put(m, "ogv", "video/ogg");
        put(m, "mov", "video/quicktime");
        put(m, "avi", "video/x-msvideo");
        put(m, "mkv", "video/x-matroska");
        put(m, "m4v", "video/x-m4v");
        // 文档 / 办公
        put(m, "pdf", "application/pdf");
        put(m, "doc", "application/msword");
        put(m, "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        put(m, "xls", "application/vnd.ms-excel");
        put(m, "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        put(m, "ppt", "application/vnd.ms-powerpoint");
        put(m, "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        put(m, "csv", "text/csv");
        put(m, "md", "text/markdown");
        put(m, "markdown", "text/markdown");
        // 文本 / Web
        put(m, "txt", "text/plain");
        put(m, "html", "text/html");
        put(m, "htm", "text/html");
        put(m, "css", "text/css");
        put(m, "js", "text/javascript");
        put(m, "mjs", "text/javascript");
        put(m, "json", "application/json");
        put(m, "jsonl", "application/x-ndjson");
        put(m, "xml", "application/xml");
        put(m, "yaml", "application/yaml");
        put(m, "yml", "application/yaml");
        // 压缩包
        put(m, "zip", "application/zip");
        put(m, "rar", "application/vnd.rar");
        put(m, "7z", "application/x-7z-compressed");
        put(m, "tar", "application/x-tar");
        put(m, "gz", "application/gzip");
        EXTENSION_TO_MIME = Collections.unmodifiableMap(m);
    }

    private FileExtensionMimeTypeUtils() {
    }

    private static void put(Map<String, String> map, String ext, String mime) {
        map.put(ext, mime);
    }

    /**
     * 从文件名或单独的后缀解析 MIME 类型。
     * <p>
     * 支持传入完整路径或文件名（取最后一个 {@code .} 后的扩展名），也支持仅传入扩展名（可带或不带前导 {@code .}）。
     * </p>
     *
     * @param filenameOrExtension 如 {@code report.pdf}、{@code .png}、{@code mp3}
     * @return 非 null；无法识别时为 {@value #DEFAULT_MIME_TYPE}
     */
    public static String getMimeTypeFromExtension(String filenameOrExtension) {
        String ext = normalizeExtension(filenameOrExtension);
        if (ext.isEmpty()) {
            return DEFAULT_MIME_TYPE;
        }
        String fromMap = EXTENSION_TO_MIME.get(ext);
        if (fromMap != null) {
            return fromMap;
        }
        String guessed = URLConnection.guessContentTypeFromName("file." + ext);
        if (guessed != null && !guessed.isBlank()) {
            return guessed;
        }
        return DEFAULT_MIME_TYPE;
    }

    /**
     * 仅保留小写扩展名（不含点）；非法或空输入返回空字符串。
     */
    public static String normalizeExtension(String filenameOrExtension) {
        if (filenameOrExtension == null || filenameOrExtension.isBlank()) {
            return "";
        }
        String s = filenameOrExtension.trim();
        int slash = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
        if (slash >= 0 && slash < s.length() - 1) {
            s = s.substring(slash + 1);
        }
        int dot = s.lastIndexOf('.');
        if (dot < 0 || dot == s.length() - 1) {
            if (s.startsWith(".") && s.length() > 1) {
                return s.substring(1).toLowerCase(Locale.ROOT);
            }
            return s.toLowerCase(Locale.ROOT);
        }
        return s.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
