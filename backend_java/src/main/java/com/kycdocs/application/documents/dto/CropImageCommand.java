package com.kycdocs.application.documents.dto;

public record CropImageCommand(
    int left,
    int top,
    int width,
    int height
) {}
