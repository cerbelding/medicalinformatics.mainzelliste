/*
 * Copyright (C) 2013-2016 Martin Lablans, Andreas Borg, Frank Ückert and contributors
 * (see Git commit history for individual contributions)
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
package de.pseudonymisierung.mainzelliste.matcher.hasher;

import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.dto.Persistor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Transient;

/**
 * Allows to formate the specified input data for generate hashes.
 */
@Entity
public class HashFormatter {

    /**
     * Instance of a HashFormatter
     */
    private static HashFormatter instance;

    /**
     * List which represents a format
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "Hashformatter", joinColumns = @JoinColumn(name = "format_id"))
    @Column(name = "format", table = "HashFormatterList")
    private List<String> format;

    @Transient
    private boolean formatChanged;

    /** Database id */
    @Id
    private final Long id = 1L;

    /**
     * Generates a HashFormatter. A format is created based on the specified 
     * input data in the configuration file. Input data can be contain names of 
     * fields or any combinations of characters. If a specified value is empty, 
     * it is interpreted as a space.
     * <br>
     * <br>
     * Example:<br>
     * <code>
     * Hash.input.field.0 = vorname    -> Field name
     * Hash.input.field.1 =            -> Space
     * Hash.input.field.2 = nachname   -> Field name
     * Hash.input.field.3 = _          -> _
     * Hash.input.field.4 = 1A         -> 1A (character combination)
     *
     * Resulting format: 'vorname nachname_1A'
     * </code>
     *
     * If a value is recognized as a field name, the value of the field is set.<br>
     * Example: 'HANS MEIER_1A'
     */
    private HashFormatter() {
        formatChanged = true;
    }

    /**
     * Read the specified input data and generate a list, which represents the
     * format. The number in the key is the position in the format. The first
     * position is 0. If a number is forgotten, all following positions are not
     * considered.<br>
     * <br>
     * Example:<br>
     * <code>
     * hash.input.field.0 = vorname
     * hash.input.field.1 =
     * hash.input.field.2 = nachname
     * Resulting format: 'vorname nachname'
     *
     * # Position 1 was forgotten
     * hash.input.field.0 = vorname
     * hash.input.field.2 =
     * hash.input.field.3 = nachname
     * Resulting format: 'vorname'
     * </code>
     *
     * @return List, which represents the read format.
     */
    private List<String> readFormat() {
               
        List<String> tmpFormat = new ArrayList<String>();

        for (int i = 0;; ++i) {
            String currentVal = Config.instance.getProperty("hash.input.field." + i);

            if (currentVal != null) {
                if (currentVal.isEmpty()) {
                    currentVal = " ";
                }

                tmpFormat.add(currentVal);
            } else {
                break;
            }
        }

        return tmpFormat;
    }

    /**
     * Returns an instance of the HashFormatter. If an instance exists, this
     * instance is returned. Otherwise, the instance is read from the database
     * or newly created if no instance has been persisted.
     *
     * @return Instance of the HashFormatter
     */
    public static HashFormatter getInstance() {
        if (instance == null) {
        	HashFormatter persistedInstance = Persistor.instance.getHashFormatter();
            if (persistedInstance == null)
            	instance = new HashFormatter();
            else instance = persistedInstance;
        }

        return instance;
    }

    /**
     * Initialize the attributes for checking whether the HashFormatter format
     * was changing after a restart of Mainzelliste.
     * 
     * This call is only needed once.
     */
    public void initialize() {
        List<String> currentFormat = readFormat();
        
        if (!currentFormat.equals(format)) {
            formatChanged = true;
            format = currentFormat;
        } else {
            formatChanged = false;
        }
    }

    /**
     * Generate a string based of the created format. Names of fields are
     * replaced by the respective value. The values are set based on the given
     * fields.<br>
     * <br>
     * Example:<br>
     * Fields: {vorname:HANS, nachname:MEIER}<br>
     * Format: 'vorname nachname' Resulting string: 'HANS MEIER'
     *
     * @param fields Fields with values.
     *
     * @return Generated string based on the given fields.
     */
    public String getHasherInput(Map<String, Field<?>> fields) {
        String inputStr = "";

        for (String key : format) {
            Field<?> tmpField = fields.get(key);

            if (tmpField == null) {
                inputStr += key;
            } else {
                inputStr += tmpField.getValue();
            }
        }

        return inputStr;
    }

    /**
     * Returns whether the format of hashing was changing after the last start.
     *
     * @return true if it was changed, false if it was not.
     */
    public boolean isHashFormatChanged() {
        return formatChanged;
    }
}
