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
/**
 *
 */
package de.pseudonymisierung.mainzelliste;

import de.pseudonymisierung.mainzelliste.crypto.Encryption;
import de.pseudonymisierung.mainzelliste.exceptions.GeneralCryptoException;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidConfigurationException;
import de.pseudonymisierung.mainzelliste.exceptions.NotImplementedException;

import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.Properties;

/**
 * Simple ID generator that outputs consecutive IntegerIDs. For testing purposes
 * or to produce IDs for a database.
 */
public class CryptoIDGenerator implements DerivedIDGenerator<CryptoID> {

  /** The ID type this generator instance creates. */
  private String idType;
  private String baseIdType;
  private Encryption symEncryption;

  @Override
  public void init(IDGeneratorMemory mem, String idType, String[] eagerGenRelatedIdTypes, Properties props) {
    this.idType = idType;
    this.baseIdType = props.getProperty("baseIdType");
    if (baseIdType.isEmpty()) {
      throw new InvalidConfigurationException("Base ID type for CryptoID <"+idType+"> wasn't configured");
    }
    String encryptionName = props.getProperty("symEncryption");
    if (encryptionName.isEmpty()) {
      throw new InvalidConfigurationException("Symmetric encryption for CryptoID <"+idType+"> wasn't configured");
    }
    this.symEncryption = Config.instance.getEncryption(encryptionName);
  }

  @Override
  public synchronized CryptoID getNext() {
  	throw new NotImplementedException("Cannot get next ID for crypto ID type!");
  }

  @Override
  public CryptoID computeId(ID baseId) { return new CryptoID(encode(baseId.getIdString()), idType); }

  @Override
  public CryptoID buildId(String id) {
		return new CryptoID(id, this.idType);
	}

  @Override
  public boolean verify(String id) { return true; }

  @Override
  public String correct(String idString) { throw new NotImplementedException("Cannot correct external ID!"); }

  @Override
  public String getIdType() {
		return idType;
	}

  @Override
  public boolean isExternal() { return false; }

  @Override
  public boolean isPersistent() { return false; }

  @Override
  public Optional<IDGeneratorMemory> getMemory() {
		return Optional.empty();
	}

  @Override
  public boolean isEagerGenerationOn(String idType) {
		return false;
	}

  @Override
  public String getBaseIdType() { return baseIdType; }

  @Override
  public ID getBaseId( CryptoID derivedId ) {
  	return IDGeneratorFactory.instance.buildId(baseIdType, decode(derivedId.idString));
  }

  /**
   * Encrypts string with a symmetric encryption method
   *
   * @param sourceString source string for encryption
   * @return encrypted string in Base64String format
   */
  private String encode( String sourceString ) {
    try {
      return symEncryption.encryptToBase64String(sourceString);
    } catch (GeneralSecurityException e) {
      throw new GeneralCryptoException(
          "Computing Crypto ID with IDtype " + this.idType + " failed", e);
    }
  }

  /**
   * Decrypts encrypted string with a symmetric encryption method
   *
   * @param encodedString source string for encryption
   * @return decrypted string
   */
  private String decode( String encodedString ) {
    try {
      return symEncryption.decryptToString(encodedString);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      throw new GeneralCryptoException(
        "Computing Base ID with IDtype " + this.baseIdType + " failed.", e);
    }
  }
}
