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
import java.util.Collection;
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

/**
 * An externally generated patient identifier. Imported in Mainzelliste from
 * external systems (cannot be internally generated or overwritten).
 */
@Entity
public class AssociatedIds extends Identifiable {

	@Id
	@GeneratedValue
	private int jpaid;

	//TODO can be transient: the type can be determined in onload method
	@Basic
	private String type;

	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
	protected Set<ID> ids = new HashSet<>();

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

	@Override
	protected Set<ID> getInternalIds() {
		return ids;
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

	@Override
	public void setTentative(boolean isTentative) {
		this.ids.forEach(id -> id.setTentative(isTentative));
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

	@Override
	protected IDGenerator<? extends ID> getIDGeneratorFactory(String idType) {
		return IDGeneratorFactory.instance.getAssociatedIdGenerators(type).stream()
				.filter(g -> g.getIdType().equals(idType))
				.findFirst()
				.orElse(null);
	}

	@Override
	protected Collection<IDGenerator<? extends ID>> getNonExternalIdGenerators() {
		return IDGeneratorFactory.instance.getNonExtAssociatedIdGenerators(type);
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
