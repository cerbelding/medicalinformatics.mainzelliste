package de.pseudonymisierung.mainzelliste.crypto;

import de.pseudonymisierung.mainzelliste.crypto.key.CryptoKey;
import de.pseudonymisierung.mainzelliste.crypto.key.KeyType;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import org.testng.Assert;
import org.testng.annotations.Test;

public class JCEAsymmetricEncryptionTest {

  final static private String CRYPTO_KEY_FOLDER = "./ci/newman_mainzelliste_resources/keys/";

  @Test
  public void test_encrypt() throws GeneralSecurityException, IOException {
    //prepare keys and encryption
    CryptoKey rsaPublicKey = CryptoUtil
        .readKey(KeyType.RSA_PUBLIC.name(),
            Files.readAllBytes(new File(CRYPTO_KEY_FOLDER + "rsa_public.der").toPath()));
    PrivateKey rsaPrivateKey = KeyFactory.getInstance("RSA")
        .generatePrivate(new PKCS8EncodedKeySpec(
            Files.readAllBytes(new File(CRYPTO_KEY_FOLDER + "rsa_private.der").toPath())));
    Encryption encryption = CryptoUtil.createEncryption(EncryptionType.RSA_ENCRYPT.name(), rsaPublicKey);

    // test encryption
    String plainText = "Test Text";
    String base64CipherText = encryption.encryptToBase64String(plainText);

    // try to decrypt
    Cipher decryptionCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
    decryptionCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
    byte[] decryptedTextBytes = decryptionCipher.doFinal(Base64.getUrlDecoder().decode(base64CipherText));
    String decryptedText = new String(decryptedTextBytes, StandardCharsets.UTF_8);

    Assert.assertEquals(decryptedText, plainText);
  }
}
