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

import de.pseudonymisierung.mainzelliste.exceptions.NotImplementedException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Pseudo generator for externally generated patient identifiers.
 */
public class ForkIDGenerator implements IDGenerator<ForkID> {

    /** The ID type this generator instance creates. */
    String idType;
    private Properties props;

    @Override
    public void init(IDGeneratorMemory mem, String idType, String[] eagerGenRelatedIdTypes,
        Properties props) {
        this.idType = idType;
        this.props = props;
    }

    /**
     * Verify an external ID. Always returns true as Mainzelliste cannot
     * recognize illegal external IDs.
     * 
     * @return true
     */
    @Override
    public boolean verify(String id) {
        return true;
    }

    /**
     * Not implemented for external IDs.
     * 
     * @throws NotImplementedException
     */
    @Override
    public synchronized ForkID getNext() {
        throw new NotImplementedException("Cannot get next ID for external ID type!");
    }

    /**
     * Not implemented for external IDs.
     * 
     * @throws NotImplementedException
     */
    @Override
    public String correct(String IDString) {
        throw new NotImplementedException("Cannot correct external ID!");
    }

    @Override
    public ForkID buildId(String id) {
        return new ForkID(id, getIdType());
    }

    @Override
    public String getIdType() {
        return idType;
    }

    @Override
	public boolean isExternal() { return true; }

    @Override
    public boolean isPersistent() { return true; }

    @Override
    public Optional<IDGeneratorMemory> getMemory() {
        return Optional.empty();
    }

    @Override
    public boolean isEagerGenerationOn(String idType) {
        return false;
    }

    public Set<String> availableIdTypes() {
        Set<String> idTypes = new HashSet<String>();
        for(String idType: this.props.getProperty("idgenerators").split(",")) {
            idTypes.add(idType.trim());
        }
        return idTypes;
    }

    public boolean holdsIdType(String idType) {
        return this.availableIdTypes().contains(idType);
    }
}
