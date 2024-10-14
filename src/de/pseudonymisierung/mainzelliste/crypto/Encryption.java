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

import java.security.GeneralSecurityException;

public interface Encryption {

  /**
   * return cipher text
   *
   * @param plaintext plain text
   * @return cipher text
   */
  byte[] encrypt(String plaintext) throws GeneralSecurityException;

  /**
   * return a URL-safe base 64 cipher text
   *
   * @param plaintext plain text
   * @return resulting a URL-safe base 64 text
   */
  String encryptToBase64String(String plaintext) throws GeneralSecurityException;

  /**
   * return cipher text
   *
   * @param plaintext plain text
   * @return cipher text
   */
  byte[] decrypt(String plaintext) throws GeneralSecurityException;

  /**
   * return decrypted plain text
   *
   * @param plaintext plain text
   * @return decrypted plain text
   */
  String decryptToString(String plaintext) throws GeneralSecurityException;
}
