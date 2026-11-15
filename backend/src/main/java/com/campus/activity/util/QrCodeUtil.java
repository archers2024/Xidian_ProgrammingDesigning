package com.campus.activity.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码工具：生成 PNG 字节流，供宣传页报名码与签到码复用。
 */
public class QrCodeUtil {

    public static byte[] png(String content, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new MultiFormatWriter()
                .encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }
}
