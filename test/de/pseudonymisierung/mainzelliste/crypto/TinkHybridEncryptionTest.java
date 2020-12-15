package de.pseudonymisierung.mainzelliste.crypto;

import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.KeysetHandle;
import de.pseudonymisierung.mainzelliste.crypto.key.CryptoKey;
import de.pseudonymisierung.mainzelliste.crypto.key.KeyType;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Base64;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TinkHybridEncryptionTest {

  final static private String CRYPTO_KEY_FOLDER = "./ci/newman_mainzelliste_resources/keys/";

  @Test
  public void test_encrypt() throws GeneralSecurityException, IOException {
    //prepare keys and encryption
    CryptoKey tinkHybridPublicKey = CryptoUtil
        .readKey(KeyType.TINK_KEYSET.name(),
            Files.readAllBytes(new File(CRYPTO_KEY_FOLDER + "tink_hybrid_public.json").toPath()));
    CryptoKey tinkHybridPrivateKey = CryptoUtil
        .readKey(KeyType.TINK_KEYSET.name(),
            Files.readAllBytes(new File(CRYPTO_KEY_FOLDER + "tink_hybrid_private.json").toPath()));
    Encryption encryption = CryptoUtil.createEncryption(EncryptionType.TINK_HYBRID_ENCRYPT.name(), tinkHybridPublicKey);

    // test encryption
    String plainText = "Test Text";
    String base64CipherText = encryption.encryptToBase64String(plainText);

    // try to decrypt
    byte[] decryptedTextBytes = tinkHybridPrivateKey.getKey(KeysetHandle.class)
        .getPrimitive(HybridDecrypt.class)
        .decrypt(Base64.getUrlDecoder().decode(base64CipherText), null);
    String decryptedText = new String(decryptedTextBytes, StandardCharsets.UTF_8);

    Assert.assertEquals(decryptedText, plainText);
  }
}
