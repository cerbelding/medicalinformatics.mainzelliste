package de.pseudonymisierung.mainzelliste;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import de.pseudonymisierung.mainzelliste.dto.Persistor;

public class AssociatedIdsFactory {
    private static AssociatedIdsFactory instance;

    private final Map<String, Set<String>> associatedIdTypes = new HashMap<>();

    /**
     * Create a Factory with properties from the Config Singleton instnace.
     */
    protected AssociatedIdsFactory() {
        this(Config.instance.getProperties());
    }

    /**
     * Create a Factory with properties supplied.
     * @param props Mainzelliste properties, where relevant properties are extracted from.
     */
    protected AssociatedIdsFactory(Properties props) {
        for(String propKey: props.stringPropertyNames()) {
            if(propKey.startsWith("associatedids.") && propKey.endsWith(".idgenerators")) {
                String idType = propKey.substring("associatedids.".length(), propKey.length() - ".idgenerators".length());
                Set<String> idGenerators = new HashSet<String>();
                for(String propGenerator: props.getProperty("associatedids." + idType + ".idgenerators").split(",")) {
                    idGenerators.add(propGenerator.trim());
                }
                this.associatedIdTypes.put(idType, idGenerators);
            }
        }
    }

    /**
     * Returns a new factory instance.
     */
    public static AssociatedIdsFactory getInstance() {
        if(instance == null) {
            instance = new AssociatedIdsFactory();
        }
        return instance;
    }

    /**
     * Create a new and empty AssociatedIds instance based on the type.
     * @param assocType
     * @return
     */
    public AssociatedIds createAssociatedIds(String assocType) throws IllegalArgumentException {
        if(this.isAssociatedIdsType(assocType)) {
            return new AssociatedIds(assocType);
        }
        throw new IllegalArgumentException("There is no AssociatedIds of type " + assocType);
    }

    /**
     * Create a new and empty AssociatedIds instance based on a idType.
     * @param idType
     * @return
     */
    public AssociatedIds createAssociatedIdsFor(String idType) throws IllegalArgumentException {
        for(String assocType: this.associatedIdTypes.keySet()) {
            if(this.associatedIdTypes.get(assocType).contains(idType)) {
                return new AssociatedIds(assocType);
            }
        }
        throw new IllegalArgumentException("There is no AssociatedIds for id-type " + idType);
    }

    /**
     * Creates a new and empty AssociatedIds instance based on a ID instance. Use is discouraged,
     * use `getAssociatedIdsFor(ID identifier)` instead.
     * @param identifier
     * @return
     */
    public AssociatedIds createAssociatedIdsFor(ID identifier) throws IllegalArgumentException {
        return this.createAssociatedIdsFor(identifier.getType());
    }

    /**
     * Creates or retrieves an AssociatedIds instance for an ID instance. Checks the persistence
     * for an existing instance or create a new AssociatedIds instance containing `identifier`.
     * @param id
     * @return
     */
    public AssociatedIds getAssociatedIdsFor(ID id) {
        AssociatedIds newAssocId = this.createAssociatedIdsFor(id.getType());
        AssociatedIds storedAssocId = Persistor.instance.getAssociatedIdsByID(id);
        if(storedAssocId == null) {
            // TODO: eager ID generation!
            newAssocId.addId(id);
            return newAssocId;
        }
        return storedAssocId;
    }

    /**
     * Checks if `assocType` is a valid type for AssociatedIds.
     * @param assocType
     * @return
     */
    public boolean isAssociatedIdsType(String assocType) {
        return this.associatedIdTypes.keySet().contains(assocType);
    }

    /**
     * Checks if `idType` is valid to be stored in a appropriate AssociatedIds instance.
     * @param idType
     * @return
     */
    public boolean isAssociatedIdsIdType(String idType) {
        return associatedIdTypes.values().stream().anyMatch(e -> e.contains(idType));
    }

    /**
     * Checks if `identifier` has a valid idType to be stored in a appropriate AssociatedIds
     * instance.
     * @param identifier
     * @return
     */
    public boolean isAssociatedIdsID(ID identifier) {
        return this.isAssociatedIdsIdType(identifier.getType());
    }

    /**
     * Creates all non-external IDs for an AssociatedIds instance and returns it.
     * @param assocId
     * @return
     */
    public AssociatedIds createAllIdentifiers(AssociatedIds assocId) {
        if(!this.isAssociatedIdsType(assocId.getType())) {
            throw new IllegalArgumentException("The AssociatedIds type is not configured: " + assocId.getType());
        }
        for(String idType: this.associatedIdTypes.get(assocId.getType())) {
            if(assocId.getId(idType) == null) {
                IDGenerator<?> idGenerator = IDGeneratorFactory.instance.getFactory(idType);
                if(!idGenerator.isExternal()) {
                    assocId.addId(idGenerator.getNext());
                }
            }
        }
        return assocId;
    }
}
