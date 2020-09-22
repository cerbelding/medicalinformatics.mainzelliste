/*
 * Copyright (C) 2013-2015 Martin Lablans, Andreas Borg, Frank Ückert
 * Contact: info@mainzelliste.de
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with Jersey (https://jersey.java.net) (or a modified version of that
 * library), containing parts covered by the terms of the General Public
 * License, version 2.0, the licensors of this Program grant you additional
 * permission to convey the resulting work.
 */
package de.pseudonymisierung.mainzelliste.crypto;

import de.pseudonymisierung.mainzelliste.crypto.key.CryptoKey;
import de.pseudonymisierung.mainzelliste.crypto.key.JCEKey;
import de.pseudonymisierung.mainzelliste.crypto.key.KeyType;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class CryptoUtil {

  private CryptoUtil() {
    throw new IllegalStateException("Utility class");
  }

  public static Encryption createEncryption(String encryptionType, CryptoKey wrappedKey)
      throws InvalidKeySpecException {
    try {
      switch (EncryptionType.valueOf(encryptionType.trim())) {
        case RSA:
          PublicKey publicKey = wrappedKey.getKey(PublicKey.class);
          return new JCEAsymmetricEncryption(publicKey);
      }
    } catch (IllegalArgumentException e) {
      throw new InvalidKeySpecException("The given crypto key '" +
          wrappedKey.getKey().getClass().getSimpleName() + "' and the encryption type '" +
          encryptionType + "' are incompatible ");
    } catch (GeneralSecurityException e) {
      throw new UnsupportedOperationException("can't create " +  encryptionType + " encryption instance", e);
    }
    throw new IllegalArgumentException("the given encryption type " + encryptionType + " not supported yet");
  }

  public static CryptoKey readKey(String keyType, byte[] encodedKey) {
    if (KeyType.valueOf(keyType.trim()) == KeyType.RSA_PUBLIC) {
      try {
        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encodedKey));
        return new JCEKey(key);
      } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
        throw new UnsupportedOperationException(e);
      }
    }
    throw new IllegalArgumentException("the given key type " + keyType + " not supported yet");
  }
}
