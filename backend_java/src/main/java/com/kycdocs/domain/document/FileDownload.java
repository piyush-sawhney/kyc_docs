package com.kycdocs.domain.document;

public record FileDownload(
    byte[] data,
    String mimeType,
    String originalFilename
) {}
