package com.greathiit.bwc.common;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 简易 PDF 生成：通行码、审批单、整改通知单、失物标签、月报。
 * 中文字体优先读取 bwc.pdf-font（默认 /usr/share/fonts/chinese/stsong.ttf）。
 */
public final class PdfUtil {

    private PdfUtil() {
    }

    public static byte[] generate(String title, List<String> lines, String fontPath) throws IOException {
        return generate(title, lines, null, fontPath);
    }

    /**
     * 带二维码的 PDF（通行码、失物标签等）。
     */
    public static byte[] generate(String title, List<String> lines, byte[] qrPng, String fontPath)
            throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDFont font = loadFont(document, fontPath);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(font, 18);
                stream.newLineAtOffset(60, 760);
                stream.showText(title);
                stream.endText();
                float y = 720;
                for (String line : lines) {
                    stream.beginText();
                    stream.setFont(font, 11);
                    stream.newLineAtOffset(60, y);
                    stream.showText(line == null ? "" : line.replaceAll("[\\r\\n]", " "));
                    stream.endText();
                    y -= 22;
                    if (y < 60) {
                        break;
                    }
                }
                if (qrPng != null && qrPng.length > 0) {
                    org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject image =
                            org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
                                    .createFromByteArray(document, qrPng, "qr.png");
                    stream.drawImage(image, 400, 500, 120, 120);
                }
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static PDFont loadFont(PDDocument document, String fontPath) throws IOException {
        List<String> candidates = new ArrayList<>();
        if (fontPath != null && !fontPath.isBlank()) {
            candidates.add(fontPath);
        }
        candidates.add("/usr/share/fonts/chinese/stsong.ttf");
        candidates.add("C:/Windows/Fonts/simhei.ttf");
        candidates.add("C:/Windows/Fonts/simfang.ttf");
        candidates.add("C:/Windows/Fonts/simsunb.ttf");
        for (String path : candidates) {
            if (Files.exists(Path.of(path))) {
                try {
                    return PDType0Font.load(document, new File(path));
                } catch (IOException | RuntimeException ignored) {
                    // 尝试下一个候选字体
                }
            }
        }
        return org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;
    }
}
