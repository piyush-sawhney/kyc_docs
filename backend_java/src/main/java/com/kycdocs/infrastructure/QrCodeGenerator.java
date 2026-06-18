package com.kycdocs.infrastructure;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Component
public class QrCodeGenerator {

    private static final int QR_SIZE = 300;

    public String generateDataUrl(String content) {
        try {
            var writer = new QRCodeWriter();
            var bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            var bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            var baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);

            var base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
}
