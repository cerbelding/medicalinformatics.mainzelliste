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
package de.pseudonymisierung.mainzelliste.matcher;

import java.util.BitSet;
import java.util.Collection;
import java.util.Vector;

import org.apache.commons.codec.digest.DigestUtils;

import de.pseudonymisierung.mainzelliste.HashedField;
import de.pseudonymisierung.mainzelliste.PlainTextField;

/**
 * BloomFilterTransformer and DiceFieldComparator implement the method presented
 * by Schnell et al (2009). BloomFilterTransformer splits a string enclosed in a
 * PlainTextField into n-grams
 *
 * @see <a href="http://www.biomedcentral.com/1472-6947/9/41">Rainer Schnell,
 *      Tobias Bachteler and Jörg Reiher: Privacy-preserving record linkage
 *      using Bloom filters. BMC Medical Informatics and Decision Making 2009,
 *      9:41</a>
 */
public class BloomFilterTransformer extends FieldTransformer<PlainTextField, HashedField> {

	/** Bit length of the bloom filter. */
	public final static int hashLength = 500;
	/** Length of n-grams by which input fields are encoded. */
	private int nGramLength = 2;
	/** Number of hash functions. See cited article for details. */
	private int nHashFunctions = 15;

	/**
	 * Split the input string into n-grams of length nGramLength. The string is
	 * padded with nGramLength-1 spaces (trailing and leading).
	 *
	 * For example, the input string "Java" yields the output n-grams " J",
	 * "Ja", "av", "va", "a ".
	 *
	 * @param input String to split into n-grams.
	 * @return The set of n-grams.
	 */
	private Collection<String> getNGrams(String input){
		// initialize Buffer to hold input and padding
		// (nGramLength - 1 spaces on each side)
		StringBuffer buffer = new StringBuffer(input.length() + 2 * (nGramLength - 1));
		// Add leading padding
		for (int i = 0; i < nGramLength - 1; i++)
			buffer.append(" ");
		// add input string
		buffer.append(input);
		// add leading padding
		for (int i = 0; i < nGramLength - 1; i++)
			buffer.append(" ");

		Vector<String> output = new Vector<String>(buffer.length() - nGramLength + 1);
		for (int i = 0; i <= buffer.length() - nGramLength; i++)
		{
			output.addElement(buffer.substring(i, i + nGramLength));
		}
		return output;
	}

	/**
	 * Backend method for computing hash functions. See cited article for details.
	 * @param input The string to hash.
	 * @param index Index of the hash function.
	 * @return The bit in the bloom filter that is set by applying the hash function.
	 */
	private int hash(String input, int index)
	{
		int hash1 = 0;
		int hash2 = 0;

		byte inputBytes[] = input.getBytes();
		byte md5[] = DigestUtils.md5(inputBytes);
		byte sha[] = DigestUtils.sha1(inputBytes);

		// calculate significant Bytes of Hash
		int nSignBytes = (int) Math.ceil(Math.log(hashLength) / Math.log(256));

		// calculate combined Hash
		for (int byteInd = 0; byteInd < nSignBytes; byteInd++)
		{
			// byte is signed (-128 - 127), add 128 to get an unsigned value
			hash1 += Math.pow(256, byteInd) * (md5[md5.length - 1 - byteInd] + 128);
			hash2 += Math.pow(256, byteInd) * (sha[sha.length - 1 - byteInd] + 128);
		}

		return (hash1 + index * hash2) % hashLength;
	}


	@Override
	public HashedField transform(PlainTextField input)
	{
		BitSet bitSet = new BitSet(hashLength);
		Collection<String> nGrams = getNGrams(input.getValue());
		for (String nGram : nGrams)
		{
			for (int i = 0; i < nHashFunctions; i++)
			{
				int hashRet = hash(nGram, i);
				bitSet.set(hashRet);
			}
		}

		HashedField output = new HashedField(bitSet);
		return output;
	}

	@Override
	public Class<PlainTextField> getInputClass()
	{
		return PlainTextField.class;
	}

	@Override
	public Class<HashedField> getOutputClass()
	{
		return HashedField.class;
	}
}
