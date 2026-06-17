package com.kycdocs.service.document.impl;

import com.kycdocs.domain.document.EncryptedData;
import com.kycdocs.infrastructure.encryption.Aes256GcmService;
import com.kycdocs.service.document.DocumentEncryptionService;
import org.springframework.stereotype.Component;

@Component
public class DocumentEncryptionServiceImpl implements DocumentEncryptionService {

    private final Aes256GcmService encryptionService;

    public DocumentEncryptionServiceImpl(Aes256GcmService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public EncryptedData encryptDocument(byte[] fileData) {
        return encryptionService.encrypt(fileData);
    }

    @Override
    public byte[] decryptDocument(EncryptedData encryptedData) {
        return encryptionService.decrypt(encryptedData);
    }

    @Override
    public String encryptDocumentNumber(String documentNumber) {
        return encryptionService.encryptString(documentNumber);
    }

    @Override
    public String decryptDocumentNumber(String encryptedDocumentNumber) {
        return encryptionService.decryptString(encryptedDocumentNumber);
    }
}
