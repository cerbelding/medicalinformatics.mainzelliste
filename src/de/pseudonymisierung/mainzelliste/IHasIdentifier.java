package de.pseudonymisierung.mainzelliste;

import java.util.Set;

public interface IHasIdentifier {
    /**
     * Analogous to Patient.createId(String type)
     * @param idType
     * @return
     */
    public ID createId(String idType);
    /**
     * Analogous to Patient.getId(String type)
     * @param idType
     * @return
     */
    public ID getId(String idType);
    /**
     * Analogous to Patient.addId(ID id)
     * @param identifier
     * @return
     */
    public boolean addId(ID identifier);
    /**
     * Analogous to Patient.getIds().
     * @return
     */
    public Set<ID> getIds();
    /**
     * Removes the ID of type idType from the objects IDs.
     * @param idType
     * @return
     */
    public boolean removeId(String idType);
}
