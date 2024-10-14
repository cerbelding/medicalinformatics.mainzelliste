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

import com.google.crypto.tink.DeterministicAead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.daead.DeterministicAeadConfig;
import de.pseudonymisierung.mainzelliste.crypto.key.CryptoKey;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class TinkDeterministicAeadEncryption extends AbstractTinkEncryption<DeterministicAead> {

  private static final Logger logger = LogManager.getLogger(TinkDeterministicAeadEncryption.class);

  static {
    try {
      DeterministicAeadConfig.register();
    } catch (GeneralSecurityException e) {
      logger.fatal("Couldn't register key managers to handle supported DeterministicAeas Tink-key types", e);
      throw new InternalErrorException(e);
    }
  }

  public TinkDeterministicAeadEncryption(CryptoKey key)
      throws GeneralSecurityException {
    super(key, DeterministicAead.class);
  }

  public TinkDeterministicAeadEncryption(KeysetHandle keysetHandle)
      throws GeneralSecurityException {
    super(keysetHandle, DeterministicAead.class);
  }

  @Override
  public byte[] encrypt(String plaintext) throws GeneralSecurityException {
    return primitive.encryptDeterministically(plaintext.getBytes(StandardCharsets.UTF_8), null);
  }

  @Override
  public String encryptToBase64String(String plaintext) throws GeneralSecurityException {
    return Base64.encodeBase64URLSafeString(encrypt(plaintext));
  }

  @Override
  public byte[] decrypt(String plaintext) throws GeneralSecurityException {
    return primitive.decryptDeterministically(Base64.decodeBase64(plaintext.getBytes(StandardCharsets.UTF_8)), null);
  }

  @Override
  public String decryptToString(String plaintext) throws GeneralSecurityException {
    return new String(decrypt(plaintext));
  }
}
