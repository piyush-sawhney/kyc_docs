package com.kycdocs.application.auth.dto;

public record LoginInitResult(
    boolean enrolled,
    String enrollToken,
    String qrDataUrl
) {}
