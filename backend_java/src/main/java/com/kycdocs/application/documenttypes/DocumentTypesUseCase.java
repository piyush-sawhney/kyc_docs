package com.kycdocs.application.documenttypes;

import com.kycdocs.domain.documenttype.DocumentType;

import java.util.List;

public interface DocumentTypesUseCase {

    List<DocumentType> listAll();

    DocumentType create(String name);

    DocumentType update(String id, String name);
}
