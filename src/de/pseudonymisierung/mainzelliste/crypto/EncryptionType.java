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

public enum EncryptionType {
  /**
   * JCE asymmetric encryption with ECB/OAEPWithSHA-1AndMGF1Padding
   */
  RSA_ENCRYPT,

  /**
   * JCE asymmetric decryption
   */
  RSA_DECRYPT,

  /**
   * Hybrid encryption with Tink (ECIES with AEAD and HKDF).
   * The plaintext is encrypted with a new generated symmetric key and the asymmetric public key is
   * used to encrypt the symmetric key only.
   * ciphertext = symmetric ciphertext + encrypted symmetric key.
   */
  TINK_HYBRID_ENCRYPT,

  /**
   * Hybrid decryption with Tink
   */
  TINK_HYBRID_DECRYPT,

  /**
   * Symmetric deterministic encryption with Tink (AEAD: AES-SIV).
   * The plaintext is encrypted with a symmetric key
   */
  TINK_DETERMINISTIC
}