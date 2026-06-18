package com.kycdocs.application.documents.dto;

public record UpdateDocumentMetadataCommand(
    String documentTypeId,
    String issueDate,
    String expiryDate
) {}
