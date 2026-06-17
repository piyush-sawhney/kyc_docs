package com.kycdocs.service.imaging;

import java.util.Map;

public interface ImageProcessingService {

    ProcessResult processUpload(byte[] data, String mimeType, String filename);

    byte[] rotate(byte[] data, int angle);

    byte[] crop(byte[] data, int left, int top, int width, int height);

    ProcessResult optimize(byte[] data, String mimeType);

    byte[] autoOrient(byte[] data);

    record ProcessResult(byte[] data, Map<String, Object> metadata) {}
}
