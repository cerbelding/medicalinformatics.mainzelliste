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

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import javax.crypto.Cipher;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

public class JCEAsymmetricEncryption implements Encryption {

  private final Logger logger = Logger.getLogger(JCEAsymmetricEncryption.class);

  private final Cipher cipher;

  public JCEAsymmetricEncryption(PublicKey publicKey) throws GeneralSecurityException {
    cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
  }

  /**
   * return base 64 cipher text
   *
   * @param input plain text
   * @return resulting base 64 text
   */
  @Override
  public String encrypt(String input) throws GeneralSecurityException {
    try {
      byte[] cipherData = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
      return Base64.encodeBase64URLSafeString(cipherData);
    } catch (GeneralSecurityException e) {
      logger.error("encryption with public rsa key failed", e);
      throw e;
    }
  }
}
