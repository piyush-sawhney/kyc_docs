package com.kycdocs.domain.document;

public enum DocumentSide {
    FRONT("front"),
    BACK("back");

    private final String value;

    DocumentSide(String value) {
        this.value = value;
    }

    public String getValue() { return value; }

    public static DocumentSide fromString(String side) {
        for (var s : values()) {
            if (s.value.equalsIgnoreCase(side)) return s;
        }
        throw new IllegalArgumentException("Unknown document side: " + side);
    }
}
