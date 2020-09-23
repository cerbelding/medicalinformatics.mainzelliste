package de.pseudonymisierung.mainzelliste.crypto;

import de.pseudonymisierung.mainzelliste.crypto.key.CryptoKey;
import de.pseudonymisierung.mainzelliste.crypto.key.KeyType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.security.spec.InvalidKeySpecException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CryptoUtilTest {

  final static private String CRYPTO_KEY_FOLDER = "./ci/newman_mainzelliste_resources/keys/";

  private CryptoKey rsaPublicKey;
  private CryptoKey tinkHybridPublicKey;
  private CryptoKey tinkHybridPrivateKey;

  @BeforeClass
  public void prepare_createEncryption_tests() throws IOException, InvalidKeySpecException {
    this.rsaPublicKey = CryptoUtil
        .readKey(KeyType.RSA_PUBLIC.name(), readFile(CRYPTO_KEY_FOLDER + "rsa_public.der"));
    this.tinkHybridPublicKey = CryptoUtil
        .readKey(KeyType.TINK_KEYSET.name(), readFile(CRYPTO_KEY_FOLDER + "tink_hybrid_public.json"));
    this.tinkHybridPrivateKey = CryptoUtil
        .readKey(KeyType.TINK_KEYSET.name(), readFile(CRYPTO_KEY_FOLDER + "tink_hybrid_private.json"));
  }

  // Test readKey(...)
  // --------------------------

  @Test
  public void test_readKey_RSA_PUBLIC() throws IOException {
    CryptoUtil.readKey(KeyType.RSA_PUBLIC.name(), readFile(CRYPTO_KEY_FOLDER + "rsa_public.der"));
  }

  @Test
  public void test_readKey_TINK_KEYSET() throws IOException {
    CryptoUtil
        .readKey(KeyType.TINK_KEYSET.name(), readFile(CRYPTO_KEY_FOLDER + "tink_hybrid_public.json"));
  }

  // Test failed calls of readKey(...)
  // -------------------------------------------

  /**
   * Invalid key type
   */
  @Test(expectedExceptions = {IllegalArgumentException.class})
  public void test_readKey_IllegalArgumentException() throws IOException {
    CryptoUtil.readKey("BAD_KEY_TYPE", readFile(CRYPTO_KEY_FOLDER + "rsa_public.der"));
  }

  /**
   * wrong key file type
   */
  @Test(expectedExceptions = {UnsupportedOperationException.class})
  public void test_readKey_RSA_PUBLIC_UnsupportedOperationException() throws IOException {
    CryptoUtil.readKey(KeyType.RSA_PUBLIC.name(), readFile(CRYPTO_KEY_FOLDER + "rsa_private.der"));
  }

  @Test(expectedExceptions = {UnsupportedOperationException.class})
  public void test_readKey_TINK_KEYSET_UnsupportedOperationException() throws IOException {
    CryptoUtil.readKey(KeyType.TINK_KEYSET.name(), readFile(CRYPTO_KEY_FOLDER + "rsa_public.der"));
  }

  // Test createEncryption(...)
  // --------------------------

  @Test
  public void test_createEncryption_RSA_PUBLIC() throws InvalidKeySpecException {
    CryptoUtil.createEncryption(EncryptionType.RSA.name(), this.rsaPublicKey);
  }

  @Test
  public void test_createEncryption_TINK_HYBRID() throws InvalidKeySpecException {
    CryptoUtil.createEncryption(EncryptionType.TINK_HYBRID.name(), this.tinkHybridPublicKey);
  }

  // Test failed calls of createEncryption(...)
  // -------------------------------------------

  @Test(expectedExceptions = {InvalidKeySpecException.class})
  public void test_createEncryption_InvalidKeySpecException_unknownKeyType() throws InvalidKeySpecException {
    CryptoUtil.createEncryption("INVALID_TYPE", this.rsaPublicKey);
  }

  /**
   * pass an incompatible key with the given encryption type
   */
  @Test(expectedExceptions = {InvalidKeySpecException.class})
  public void test_createEncryption_InvalidKeySpecException() throws InvalidKeySpecException {
    CryptoUtil.createEncryption(EncryptionType.TINK_HYBRID.name(), this.rsaPublicKey);
  }

  @Test(expectedExceptions = {UnsupportedOperationException.class})
  public void test_createEncryption_UnsupportedOperationException() throws InvalidKeySpecException {
    CryptoUtil.createEncryption(EncryptionType.TINK_HYBRID.name(), this.tinkHybridPrivateKey);
  }

  /**
   * Utils
   */

  private byte[] readFile(String keyPath) throws IOException {
    File keyFile = new File(keyPath);
    if (!keyFile.exists()) {
      throw new FileNotFoundException(String.format("Key file %s does not exist!", keyPath));
    }
    return Files.readAllBytes(keyFile.toPath());
  }
}
