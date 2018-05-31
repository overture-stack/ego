package org.overture.ego.security;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import static javax.crypto.Cipher.*;

@Slf4j
@Service
public class AESEncryptionService {

  private final String KEY;
  private final int IV_LENGTH = 16;
  private final String TRANSFORM = "AES/CBC/PKCS5PADDING";
  private Random random = new Random();

  public AESEncryptionService(@Value("${cryptoConverter.key}") String key) {
    this.KEY = key;
  }

  public String encrypt(String text) {
    try {
      // Generate random IV_LENGTH digit initVector
      byte[] randomInitVector = ByteBuffer.allocate(IV_LENGTH).putInt(random.nextInt(10 ^ IV_LENGTH)).array();

      IvParameterSpec iv = new IvParameterSpec(randomInitVector);
      SecretKeySpec skeySpec = new SecretKeySpec(KEY.getBytes("UTF-8"), "AES");

      Cipher cipher = getInstance(TRANSFORM);
      cipher.init(ENCRYPT_MODE, skeySpec,iv);

      byte[] encrypted = cipher.doFinal(text.getBytes());

      val outputStream = new ByteArrayOutputStream();
      outputStream.write(randomInitVector);
      outputStream.write(encrypted);

      return Base64.encodeBase64String(outputStream.toByteArray());
    } catch (Exception e) {
      log.error("Error encrypting text: {}", e);
    }
    return null;
  }

  public String decrypt(String encodedText) {
    try {
      byte[] decodedText = Base64.decodeBase64(encodedText);
      byte[] randomInitVector = Arrays.copyOfRange(decodedText, 0, IV_LENGTH);
      byte[] encryptedText = Arrays.copyOfRange(decodedText, IV_LENGTH, decodedText.length);

      IvParameterSpec iv = new IvParameterSpec(randomInitVector);
      SecretKeySpec skeySpec = new SecretKeySpec(KEY.getBytes("UTF-8"), "AES");

      Cipher cipher = getInstance(TRANSFORM);
      cipher.init(DECRYPT_MODE, skeySpec, iv);
      byte[] original = cipher.doFinal(encryptedText);

      return new String(original);
    } catch (Exception e) {
      log.error("Error decrypting text: {}", e);
    }

    return null;
  }
}
