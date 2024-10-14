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

/**
 * Modifications (C) 2019 Cornelius Knopp <cornelius.knopp@med.uni-goettingen.de>
 * modified IDGenerator for eGK-IDs, 
 * including the Luhn Algorithm for Validation of the German Health Insurance Number
 * designated format of an id-value: [A-Z][0-9]{9} while the last digit is a checksum
 * @author Cornelius Knopp <cornelius.knopp@med.uni-goettingen.de>
 * @date 03.12.2019
 * @version 1.2
 **/

package de.pseudonymisierung.mainzelliste;

import de.pseudonymisierung.mainzelliste.exceptions.NotImplementedException;

import java.util.Optional;
import java.util.Properties;

/**
 * Pseudo generator for externally generated patient identifiers.
 */
public class EGKIDGenerator implements IDGenerator<ExternalID>{

    /** The ID type this generator instance creates. */
    String idType;

	/**
	* Initializer of the ID Generator
	*
	* @param mem Allocated memory for the Generator
	* @param idType Type of the ID to Generate
	* @param props Properties of the Generator
	*/
    @Override
		public void init(IDGeneratorMemory mem, String idType, String[] eagerGenRelatedIdTypes,
				Properties props) {
        this.idType = idType;
    }

	@Override
	public void reset(String idType) {
		//
	}

	/**
     * Verify an external ID (eGK-ID). Returns true if value of 'id' is a valid eGK Number.
     * 
	 * @param id Value of the eGK-Number 
     * @return result of the validation
     */
    @Override
    public boolean verify(String id) {
    	return validate(id);
    }

    /**
     * Not implemented for any type of external IDs as Mainzelliste does not generate the ID.
     * 
     * @throws NotImplementedException
     */
    @Override
    public synchronized ExternalID getNext() {
        throw new NotImplementedException("Cannot get next ID for external ID type!");
    }

    /**
     * Correction-Methods are not implemented for any type of external IDs.
     * 
	 * @param IDString ID-Value to correct
     * @throws NotImplementedException
     */
    @Override
    public String correct(String IDString) {
        throw new NotImplementedException("Cannot correct external ID!");
    }

	/**
	* Builds the EGK-ID as ExternalID
	*
	* @param id Value of the eGK-Number
	* @return the generated external ID (eGK-ID)
	*/
    @Override
    public ExternalID buildId(String id) {
        return new ExternalID(id, getIdType());
    }

	/**
	* Returns the idType of eGK-ID
	*
	* @return idType of the given ID
	*/
    @Override
    public String getIdType() {
        return idType;
    }

	/**
	* Method to mark the ID as external
	*
	* @return 'External'-Flag (defaults to true)
	*/
    @Override
	public boolean isExternal() {
		 return true; 
	}

	@Override
	public boolean isPersistent() { return true; }

	@Override
	public boolean isEagerGenerationOn(String idType) {
		return false;
	}

	@Override
	public boolean isSrl() { return false; }

	@Override
	public Optional<IDGeneratorMemory> getMemory() {
		return Optional.empty();
	}

	/**
	 * Validation Algorithm to check if the given ID-value is a valid eGK-Number (German Health Insurance Number)
	 *
	 * @param input Input argument
	 * @return boolean value if 'value' matches the LUHN Algorithm
	 */
	public static boolean validate(String input) {
		//convert input-String to CharArray
		char[] chara = input.toCharArray(); 
		//initialize IntegerArray
		int[] inta = new int[10]; 
		//replace character in first place by letterCode
		int letter = chara[0]-'A'+1; 
		//for 1-digit letterCode: add leading 0
		if (letter <= 9) { 
			inta[0] = 0;
			inta[1] = letter;
		}
		// for 2-digit letterCode: split digits to 2 array-entries
		else if (letter > 9 && letter <=27) {
			inta[1] = letter % 10;
			letter /= 10;
			inta[0] = letter % 10;
		}
		// transfer digits from charArray to the IntegerArray 
		for (int i=2; i<chara.length; i++) {
			inta[i] = chara[i-1] - '0';
		}

		int checksum = Integer.valueOf(input.substring(input.length()-1, input.length()));
		return check(inta, checksum);
	}

	/**
	* Method to calculate the horizontal checksum (german = alternierende Quersumme)
	* 
	* @param number integer of which the horizontal checksum should be calculated
	* @return result of the calculation (checksum)
	*/
	public static int horizontalchecksum(int number) {
		// single-digit integers are already the checksum
		if (number <= 9) return number;
		// 2-step calculation for multi-digit values (e.g. for doubled digits)
		return number%10 + horizontalchecksum(number/10);
	}

	/**
	* Method to check the 'digitized' version of the eGK 
	*
	* @param digits the digits to check
	* @return boolean value if digits passed the validation
	*/
	public static boolean check(int[] digits, int checksum) {
	    // initialize the sum
		int sum = 0;
		// initialize lenght by length of 'digits'
	    int length = digits.length;
		// actual LUHN Algorithm
	    for (int i = 0; i < length; i=i+2)
	    {
			// double every second digit
	        digits[i+1]= digits[i+1]*2; //multiplication step
	    }
		// calculate and sum up the checksums
	    for (int i = 0; i<length; i++) { 
			// calculate checksum of each value of digits
	    	digits[i]=horizontalchecksum(digits[i]);
			// sum up all checksums
	    	sum = sum + digits[i];
	    }
		// if sum of checksums modulo 10 is true, id is a valid eGK-Number 
	    return sum % 10 == checksum;
	}
}
