package de.pseudonymisierung.mainzelliste;

import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;

/**
 * A patient identifier generated by encryption and not persisted in DB.
 * This ID type is generated from other patient ID type
 */
public class CryptoID extends ID {

  public CryptoID(String idString, String idType) throws InvalidIDException {
    super(idString, idType);
  }

  @Override
  public boolean equals(Object arg0) {
    if(!(arg0 instanceof CryptoID))
      return false;

    CryptoID other = (CryptoID)arg0;
    return other.idString.equals(idString);
  }

  @Override
  public String getIdString() {
        return idString;
    }

  @Override
  protected void setIdString(String id) throws InvalidIDException {
    if(!getFactory().verify(id))
      throw new InvalidIDException();

    idString = id;
  }
}