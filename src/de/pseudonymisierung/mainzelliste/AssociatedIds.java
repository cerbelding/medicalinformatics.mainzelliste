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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

/**
 * An externally generated patient identifier. Imported in Mainzelliste from
 * external systems (cannot be internally generated or overwritten).
 */
@Entity
public class AssociatedIds implements IHasIdentifier {

	@Basic
	protected String type;

	@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
	protected Set<ID> identifiers = new HashSet<ID>();


	public AssociatedIds() {
	}

	public AssociatedIds(String type) {
		this.type = type;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public ID createIdentifier(String idType) {
		ID newId = this.getIdentifier(idType);
		if(newId == null) {
			newId = IDGeneratorFactory.instance.getFactory(idType).getNext();
			this.identifiers.add(newId);
		}
		return newId;
	}

	@Override
	public ID getIdentifier(String idType) {
		for(ID id: this.identifiers) {
			if(id.getType().equals(idType)) {
				return id;
			}
		}
		return null;
	}

	@Override
	public boolean addIdentifier(ID identifier) {
		if(this.getIdentifier(identifier.getType()) != null) {
			return false;
		}
		return this.identifiers.add(identifier);
	}

	@Override
	public Set<ID> getIdentifiers() {
		return this.identifiers;
	}

	@Override
	public boolean removeIdentifier(String idType) {
		ID searchId = null;
		for(ID id: this.identifiers) {
			if(id.getType().equals(idType)) {
				searchId = id;
			}
		}
		return this.identifiers.remove(searchId);
	}

	@Override
	public boolean equals(Object obj) {
		// "Free" checks until we can cast
		if(obj == this) {
			return true;
		}
		if(obj == null) {
			return false;
		}
		if(!(obj instanceof AssociatedIds)) {
			return false;
		}
		AssociatedIds assocId = (AssociatedIds) obj;

		// check for same type
		if(!this.type.equals(assocId.getType())) {
			return false;
		}
		// check for same number of contained identifiers
		if(this.identifiers.size() != assocId.getIdentifiers().size()) {
			return false;
		}
		// check if each identifier is contained in the other instance
		for(ID myId: this.identifiers) {
			if(!assocId.getIdentifiers().contains(myId)) {
				return false;
			}
		}
		// same size and all identifiers are contained in assocId: this is the same!
		return true;
	}
}
