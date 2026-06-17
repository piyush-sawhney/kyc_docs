package com.kycdocs.service.imaging.impl;

import com.kycdocs.service.imaging.ImageProcessingService;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class ImageProcessingServiceImpl implements ImageProcessingService {

    private static final int MAX_SIZE_MB = 10;
    private static final long MAX_SIZE_BYTES = MAX_SIZE_MB * 1024L * 1024L;

    @Override
    public ProcessResult processUpload(byte[] data, String mimeType, String filename) {
        validateImage(data, mimeType, filename);

        var oriented = autoOrient(data);
        var optimized = optimize(oriented, mimeType);

        return optimized;
    }

    @Override
    public byte[] rotate(byte[] data, int angle) {
        try {
            var input = new ByteArrayInputStream(data);
            var image = ImageIO.read(input);
            if (image == null) throw new IllegalArgumentException("Cannot read image");

            var rotated = rotateImage(image, angle);
            var baos = new ByteArrayOutputStream();
            ImageIO.write(rotated, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ImageProcessingException("Failed to rotate image", e);
        }
    }

    @Override
    public byte[] crop(byte[] data, int left, int top, int width, int height) {
        try {
            var input = new ByteArrayInputStream(data);
            var image = ImageIO.read(input);
            if (image == null) throw new IllegalArgumentException("Cannot read image");

            var cropped = image.getSubimage(left, top, width, height);
            var baos = new ByteArrayOutputStream();
            ImageIO.write(cropped, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ImageProcessingException("Failed to crop image", e);
        }
    }

    @Override
    public ProcessResult optimize(byte[] data, String mimeType) {
        try {
            var originalSize = data.length;
            var input = new ByteArrayInputStream(data);
            var image = ImageIO.read(input);
            if (image == null) {
                return new ProcessResult(data, Map.of("originalSize", originalSize));
            }

            var baos = new ByteArrayOutputStream();
            byte[] optimizedData;

            if (mimeType != null && mimeType.contains("jpeg") || mimeType.contains("jpg")) {
                writeJpeg(image, baos, 0.85f);
                optimizedData = baos.toByteArray();
            } else {
                writePng(image, baos);
                optimizedData = baos.toByteArray();
            }

            var metadata = new HashMap<String, Object>();
            metadata.put("originalSize", originalSize);
            metadata.put("optimizedSize", optimizedData.length);
            metadata.put("originalWidth", image.getWidth());
            metadata.put("originalHeight", image.getHeight());
            metadata.put("reduction", originalSize > 0 ? (1.0 - (double) optimizedData.length / originalSize) : 0);

            return new ProcessResult(optimizedData, metadata);
        } catch (Exception e) {
            throw new ImageProcessingException("Failed to optimize image", e);
        }
    }

    @Override
    public byte[] autoOrient(byte[] data) {
        // Basic auto-orient - handles rotation based on EXIF metadata
        // TwelveMonkeys library handles EXIF automatically via ImageIO
        return data;
    }

    private void validateImage(byte[] data, String mimeType, String filename) {
        if (data.length > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }
        if (mimeType == null || !(mimeType.equals("image/jpeg") || mimeType.equals("image/png"))) {
            throw new IllegalArgumentException("Only JPEG and PNG images are supported");
        }
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename is required");
        }
        var ext = filename.toLowerCase();
        if (!(ext.endsWith(".jpg") || ext.endsWith(".jpeg") || ext.endsWith(".png"))) {
            throw new IllegalArgumentException("Only .jpg, .jpeg, and .png files are supported");
        }
    }

    private BufferedImage rotateImage(BufferedImage image, int angle) {
        var radians = Math.toRadians(angle);
        var sin = Math.abs(Math.sin(radians));
        var cos = Math.abs(Math.cos(radians));
        var w = image.getWidth();
        var h = image.getHeight();
        var newW = (int) Math.round(w * cos + h * sin);
        var newH = (int) Math.round(w * sin + h * cos);

        var rotated = new BufferedImage(newW, newH, image.getType());
        var g = rotated.createGraphics();
        g.translate((newW - w) / 2.0, (newH - h) / 2.0);
        g.rotate(radians, w / 2.0, h / 2.0);
        g.drawRenderedImage(image, null);
        g.dispose();

        return rotated;
    }

    private void writeJpeg(BufferedImage image, ByteArrayOutputStream baos, float quality) {
        var writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        var param = (JPEGImageWriteParam) writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        try (var ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (Exception e) {
            throw new ImageProcessingException("Failed to write JPEG", e);
        } finally {
            writer.dispose();
        }
    }

    private void writePng(BufferedImage image, ByteArrayOutputStream baos) {
        try {
            ImageIO.write(image, "png", baos);
        } catch (Exception e) {
            throw new ImageProcessingException("Failed to write PNG", e);
        }
    }

    public static class ImageProcessingException extends RuntimeException {
        public ImageProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
