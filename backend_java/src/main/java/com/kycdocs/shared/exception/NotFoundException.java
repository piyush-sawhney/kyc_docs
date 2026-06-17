package com.kycdocs.shared.exception;

public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message, "NOT_FOUND");
    }

    public NotFoundException(String entityType, Object id) {
        super("%s not found with id: %s".formatted(entityType, id), "NOT_FOUND");
    }
}
