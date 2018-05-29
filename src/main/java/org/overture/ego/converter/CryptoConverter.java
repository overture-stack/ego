package org.overture.ego.converter;

import org.overture.ego.security.AESEncryptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;

import javax.persistence.AttributeConverter;

@Component
@Configurable
public class CryptoConverter implements AttributeConverter<String, String> {

  private static AESEncryptionService aesEncryptionService;

  @Autowired
  public void initAESEncryptionService(AESEncryptionService aesEncryptionService) {
    CryptoConverter.aesEncryptionService = aesEncryptionService;
  }

  @Override
  public String convertToDatabaseColumn(String text) {
    return aesEncryptionService.encrypt(text);
  }

  @Override
  public String convertToEntityAttribute(String text) {
    return aesEncryptionService.decrypt(text);
  }
}
