package de.pseudonymisierung.mainzelliste;

import java.util.Set;

public interface IHasIdentifier {
    /**
     * Analogous to Patient.createId(String type)
     * @param idType
     * @return
     */
    public ID createIdentifier(String idType);
    /**
     * Analogous to Patient.getId(String type)
     * @param idType
     * @return
     */
    public ID getIdentifier(String idType);
    /**
     * Analogous to Patient.addId(ID id)
     * @param identifier
     * @return
     */
    public boolean addIdentifier(ID identifier);
    /**
     * Analogous to Patient.getIds().
     * @return
     */
    public Set<ID> getIdentifiers();
    /**
     * Removes the ID of type idType from the objects IDs.
     * @param idType
     * @return
     */
    public boolean removeIdentifier(String idType);
}
