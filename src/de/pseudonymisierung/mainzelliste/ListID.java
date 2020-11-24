package de.pseudonymisierung.mainzelliste;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;

@Entity
public class ListID extends ID {

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    protected List<ID> identifiers;

    public ListID() {
        super();
    }

    public ListID(String idType) {
        super();
        this.setType(idType);
    }

    public ListID(String idType, String idString) {
        this(idType);
        this.idString = idString;
    }

    @Override
    public String getIdString() {
        // idString is ignored, return empty String
        return this.idString; 
    }

    @Override
    protected void setIdString(String id) throws InvalidIDException {
        this.idString = id;
    }

    public List<ID> getIdList() {
        if(this.identifiers == null) {
            this.identifiers = new ArrayList<ID>();
        }
        return this.identifiers;
    }

    public ID getId(ID identifier) {
        if(this.identifiers.contains(identifier)) {
            for(ID id: this.identifiers) {
                if(id.equals(identifier)) {
                    return id;
                }
            }
        }
        return null;
    }

    public boolean removeFromIdList(ID identifier) {
        if(this.identifiers == null) {
            return false;
        }
		return this.identifiers.remove(identifier);
    }

    public boolean addToIdList(ID identifier) {
        if(this.identifiers == null) {
            this.identifiers = new ArrayList<ID>();
        }
        if(this.identifiers.contains(identifier)) {
            return false;
        }
		return this.identifiers.add(identifier);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.type + "(" + this.idString + ")=");
        if(this.identifiers == null) {
            sb.append("[]");
            return sb.toString();
        }
        sb.append("[");
        String delim = "";
        for(ID id: this.identifiers) {
            sb.append(delim);
            sb.append(id.toString());
            delim = ",";
        }
        sb.append("]");
        return sb.toString();
    }
}
