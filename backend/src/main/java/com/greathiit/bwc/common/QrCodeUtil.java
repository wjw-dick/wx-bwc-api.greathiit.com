package com.greathiit.bwc.common;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * 二维码生成（一物一码 / 通行码）。
 */
public final class QrCodeUtil {

    private QrCodeUtil() {
    }

    public static byte[] png(String content, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size,
                    Map.of(EncodeHintType.CHARACTER_SET, "UTF-8", EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException(500, "二维码生成失败");
        }
    }
}
