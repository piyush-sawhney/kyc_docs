package com.kycdocs.service.document;

import com.kycdocs.domain.document.EncryptedData;

/**
 * Interface: domain service contract for document encryption.
 */
public interface DocumentEncryptionService {

    EncryptedData encryptDocument(byte[] fileData);

    byte[] decryptDocument(EncryptedData encryptedData);

    String encryptDocumentNumber(String documentNumber);

    String decryptDocumentNumber(String encryptedDocumentNumber);
}
