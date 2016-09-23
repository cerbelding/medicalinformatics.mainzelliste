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

/**
 * Interface of a Hasher.
 * 
 * A implementation of Hasher returns a hash, based on the implemented algorithm 
 * and the given input data. Similar input data should be result similar hashes, 
 * so that the hashes could be compared. A hash comparison returns a score, which
 * represents the similarity of two hashes.
 * 
 * A hash represents a patient. Before two patients will be compared by their
 * attributes, both hashes will be compared. If this comparison is above a 
 * specified threshold, the patients will be compared by the record linkage,
 * otherwise the patients will be detected as non duplicate.
 */
public interface Hasher {

    /**
     * The respective implementation generates a hash on the basis of the
     * implemented algorithm.
     *
     * @param input input data for hash generation
     *
     * @return Generated hash
     */
    public String generate(String input);

    /**
     * The respective implementation compares two given strings and returns a
     * score, depending on the implemented algorithm. The score represents the
     * similarity of both hashes.
     *
     * @param hash1 First hash
     * @param hash2 Second hash
     *
     * @return Score which represents the similarity of both hashes.
     */
    public int compare(String hash1, String hash2);
}
