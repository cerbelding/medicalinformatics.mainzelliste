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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

/**
 * An externally generated patient identifier. Imported in Mainzelliste from
 * external systems (cannot be internally generated or overwritten).
 */
@Entity
public class AssociatedIds {

	@Id
	@GeneratedValue
	private int jpaid;

	//TODO can be transient: the type can be determined in onload method
	@Basic
	private String type;

	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
	protected Set<ID> ids = new HashSet<>();

	/**
	 * Set of IDs for this patient, which are not persisted in DB
	 */
	@Transient
	private Set<ID> transientIds = new HashSet<>();

	public AssociatedIds() {
	}

	public AssociatedIds(String type) {
		this.type = type;
	}

	public AssociatedIds(String type, List<ID> ids) {
		this(type);
		this.ids = new HashSet<>(ids);
	}

	public String getType() {
		return this.type;
	}

	// TODO duplicate code @see patient
	public ID createId(String idType) {
		ID thisId = getId(idType);
		if (thisId != null) {
			return thisId;
		}

		// ID of requested type was not found and is not external -> generate new ID
		IDGenerator<? extends ID> factory = IDGeneratorFactory.instance.getAssociatedIdGenerators(type)
				.stream().filter(g -> g.getIdType().equals(idType)).findFirst().orElse(null);
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
			IDGeneratorFactory.instance.getNonExternalIdGenerators().entrySet().stream()
					.filter(e -> e.getValue().isPersistent())
					.filter(e -> e.getValue().isEagerGenerationOn(idType))
					.filter(e -> getId(e.getKey()) == null)
					.forEach(e -> this.generateId(e.getValue()));
		}

		return generateId(factory);
	}

	// TODO duplicate code @see patient
	private ID generateId(IDGenerator<? extends ID> factory) {
		ID newID;
		if (factory.isPersistent()){
			newID = factory.getNext();
			ids.add(newID);
		} else {
			String baseIdType = ((DerivedIDGenerator<?>)factory).getBaseIdType();
			ID baseId = getId(baseIdType);
			newID = ((DerivedIDGenerator<?>)factory).computeId(baseId);
			transientIds.add(newID);
		}
		return newID;
	}

	// TODO duplicate code @see patient
	public ID getId(String idType) {
		for(ID id: this.ids) {
			if(id.getType().equals(idType)) {
				return id;
			}
		}
		return null;
	}

	// TODO duplicate code @see patient
	public boolean addId(ID identifier) {
		if(this.getId(identifier.getType()) != null) {
			return false;
		}
		return this.ids.add(identifier);
	}

	// TODO implement abstract method
	public Set<ID> getIds() {
		return this.ids;
	}

	public int getJpaId() {
		return this.jpaid;
	}

	public boolean removeId(String idType) {
		ID searchId = null;
		for(ID id: this.ids) {
			if(id.getType().equals(idType)) {
				searchId = id;
			}
		}
		return this.ids.remove(searchId);
	}

	// TODO duplicate code @see patient.updateFrom(...)
	public void updateFrom(List<ID> newIds) {
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

	public boolean contain(ID id) {
		return ids.contains(id);
	}

	public void setTentative(boolean isTentative) {
		for (ID id : this.ids) {
			id.setTentative(isTentative);
		}
	}

	@Override
	public boolean equals(Object obj) {
		// "Free" checks until we can cast
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof AssociatedIds)) {
			return false;
		}

		AssociatedIds associatedIds = (AssociatedIds) obj;

		// check for same type
		if (!this.type.equals(associatedIds.getType())) {
			return false;
		}

		return this.ids.size() == associatedIds.getIds().size() && this.ids
				.containsAll(associatedIds.getIds());
	}

	@Override
	public int hashCode() {
		return Objects.hash(jpaid);
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("AssociatedIds=[");
		String delimiter = "";
		for(ID id: this.ids) {
			str.append(delimiter);
			delimiter = ",";
			str.append(id.toString());
		}
		str.append("]");
		return str.toString();
	}

	/**
	 * Creates a new AssociatedIds instance based on the given ID instance.
	 *
	 * @param id id object
	 * @return associated id contain the given id instance
	 */
	public static AssociatedIds createAssociatedIds(ID id) {
		return new AssociatedIds(IDGeneratorFactory.instance.getAssociatedIdsType(id.getType()),
				Collections.singletonList(id));
	}
}
