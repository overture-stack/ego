package org.overture.ego.security;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
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

@Service
public class AESEncryptionService {

  private final String key;
  private final int ivLength = 16;
  private Random random = new Random();

  public AESEncryptionService(@Value("${cryptoConverter.key}") String key) {
    this.key = key;
  }

  public String encrypt(String text) {
    try {
      // Generate random ivLength digit initVector
      byte[] randomInitVector = ByteBuffer.allocate(ivLength).putInt(random.nextInt(10 ^ ivLength)).array();

      IvParameterSpec iv = new IvParameterSpec(randomInitVector);
      SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      cipher.init(Cipher.ENCRYPT_MODE, skeySpec,iv);

      byte[] encrypted = cipher.doFinal(text.getBytes());

      val outputStream = new ByteArrayOutputStream();
      outputStream.write(randomInitVector);
      outputStream.write(encrypted);

      return Base64.encodeBase64String(outputStream.toByteArray());
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public String decrypt(String encodedText) {
    try {
      byte[] decodedText = Base64.decodeBase64(encodedText);
      byte[] randomInitVector = Arrays.copyOfRange(decodedText, 0, ivLength);
      byte[] encryptedText = Arrays.copyOfRange(decodedText, ivLength, decodedText.length);

      IvParameterSpec iv = new IvParameterSpec(randomInitVector);
      SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
      byte[] original = cipher.doFinal(encryptedText);

      return new String(original);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }
}
