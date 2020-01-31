package de.pseudonymisierung.mainzelliste;

import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;

import javax.persistence.Entity;

@Entity
public class ElasticID extends ID {

    public ElasticID(String pid, String idType) throws InvalidIDException{
        super(pid, idType);
    }

    @Override
    public boolean equals(Object arg0) {
        if(!(arg0 instanceof ElasticID))
            return false;

        ElasticID other = (ElasticID)arg0;
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
