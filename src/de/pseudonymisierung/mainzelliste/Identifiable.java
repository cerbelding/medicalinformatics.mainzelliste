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
package de.pseudonymisierung.mainzelliste;

import de.pseudonymisierung.mainzelliste.exceptions.ConflictingDataException;
import de.pseudonymisierung.mainzelliste.exceptions.InvalidIDException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Transient;

public abstract class Identifiable {

  /**
   * Set of IDs for this patient, which are not persisted in DB
   */
  @Transient
  protected Set<ID> transientIds = new HashSet<>();

  /**
   * Get the ID of the specified type from this patient. The ID will be generated if it does not
   * exist and is not externally provided.
   *
   * @param idType The ID type. See {@link ID} for the general structure of an ID.
   * @return This patient's ID of the given type or null if the ID is externally provided and not
   * defined for this patient.
   * @throws InvalidIDException if the provided ID type is undefined.
   */
  public ID createId(String idType) {
    ID thisId = getId(idType);
    if (thisId != null) {
      return thisId;
    }

    // ID of requested type was not found and is not external -> generate new ID
    IDGenerator<? extends ID> factory = getIDGeneratorFactory(idType);
    if (factory == null) {
      throw new InvalidIDException("ID type " + idType + " not defined!");
    } else if (factory.isExternal()) {
      return null;
    }

    // generate ids eagerly
    // Only non external and persistent ids can be generated eagerly
    // Can lead to cycles in generating ids due to incorrect configuration
    // pid -> pid2 (eagerly), pid2 -> pid3 (eagerly), pid3 -> pid (eagerly))
    // TODO: Check for cycles in the configuration
    if (!IDGeneratorFactory.instance.isEagerGenerationOn()) {
      getNonExternalIdGenerators().stream()
          .filter(e -> !(e.getIdType()).equals(idType))
          .filter(IDGenerator::isPersistent)
          .filter(e -> e.isEagerGenerationOn(idType))
          .filter(e -> getId(e.getIdType()) == null)
          .forEach(this::generateId);
    }

    return generateId(factory);
  }

  private ID generateId(IDGenerator<? extends ID> factory) {
    ID newID;
    if (factory.isPersistent()) {
      newID = factory.getNext();
      getInternalIds().add(newID);
    } else {
      String baseIdType = ((DerivedIDGenerator<?>) factory).getBaseIdType();
      ID baseId = getId(baseIdType);
      newID = ((DerivedIDGenerator<?>) factory).computeId(baseId);
      transientIds.add(newID);
    }
    return newID;
  }

  protected abstract IDGenerator<? extends ID> getIDGeneratorFactory(String idType);

  protected abstract Collection<IDGenerator<? extends ID>> getNonExternalIdGenerators();

  /**
   * Get the ID of the specified type from this patient.
   *
   * @param idType The ID type. See {@link ID} for the general structure of an ID.
   * @return This patient's ID of the given type or null if the ID is not defined for this patient.
   * @throws InvalidIDException if the provided ID type is undefined.
   */
  public ID getId(String idType) {
    for (ID thisId : getInternalIds()) {
      if (thisId.getType().equals(idType)) {
        return thisId;
      }
    }
    return null;
  }

  /**
   * Get the already generated transient ID of the specified type from this patient.
   *
   * @param idType The ID type. See {@link ID} for the general structure of an ID.
   * @return This patient's ID of the given type or null if the ID is not defined for this patient.
   */
  public ID getTransientId(String idType) {
    for (ID thisId : transientIds) {
      if (thisId.getType().equals(idType)) {
        return thisId;
      }
    }
    return null;
  }

  /**
   * Get the set of transient IDs of this patient.
   *
   * @return The already generated transient IDs of the patient
   */
  public Set<ID> getTransientIds() {
    return Collections.unmodifiableSet(transientIds);
  }

  /**
   * Get all generated ids of this patient (persistent and transient).
   *
   * @return All already generated IDs of the patient
   */
  public Set<ID> getAllIds() {
    Set<ID> allIds = new HashSet<>(getInternalIds());
    allIds.addAll(transientIds);
    return allIds;
  }

  /**
   * Add ID if this ID type is not already in ids.
   *
   * @param id The ID to add.
   * @return true if the id was added successfully, otherwise false (if ID of this type already
   * exists).
   */
  public boolean addId(ID id) {
    if (getId(id.getType()) != null) {
      return false;
    }
    getInternalIds().add(id);
    return true;
  }

  public boolean contain(ID id) {
    return getInternalIds().contains(id);
  }

  /**
   * Get the set of IDs of this patient.
   *
   * @return The IDs of the patient as unmodifiable set. While the set itself is unmodifiable,
   * modification of the elements (ID objects) affect the patient object.
   */
  public Set<ID> getIds() {
    return Collections.unmodifiableSet(getInternalIds());
  }

  protected abstract Set<ID> getInternalIds();

  /**
   * Sets the "tentative" status of this identifiable object, i.e. if it is suspected that the
   * instance is a duplicate of another.
   *
   * @param isTentative The new tentative status.
   */
  public abstract void setTentative(boolean isTentative);

  /**
   * add the given new id, if no id with the same type are found.
   *
   * @param newIds new ids
   * @throws ConflictingDataException if at least one id with same type and different value are
   *                                  found
   */
  public void updateIds(List<ID> newIds) {
    for (ID newId : newIds) {
      ID id = getId(newId.getType());
      if (id == null) {
        addId(newId);
      } else if (!id.equals(newId)) {
        throw new ConflictingDataException(String.format("ID of type %s should be updated with "
                + "value %s but already has value %s", newId.getType(), newId.getIdString(),
            id.getIdString()));
      }
    }
  }
}
