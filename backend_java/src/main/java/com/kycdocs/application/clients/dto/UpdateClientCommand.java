package com.kycdocs.application.clients.dto;

public record UpdateClientCommand(
    String name,
    String avatar
) {}
