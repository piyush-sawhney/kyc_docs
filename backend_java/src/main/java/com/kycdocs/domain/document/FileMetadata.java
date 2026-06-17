package com.kycdocs.domain.document;

public record FileMetadata(
    String originalFilename,
    String mimeType,
    int fileSize,
    Integer width,
    Integer height,
    Integer rotation
) {

    public FileMetadata {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Original filename must not be blank");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("MIME type must not be blank");
        }
        if (fileSize <= 0) {
            throw new IllegalArgumentException("File size must be positive");
        }
    }
}
