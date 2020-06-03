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
 *
 *
 * This file is a Java port of the PID generation code by Klaus Pommerening.
 * The original copyright notice follows:
 *
 **** PIDgen.c *************************************************
 *                                                             *
 * Functions to support a pseudonymization service             *
 *-------------------------------------------------------------*
 * Klaus Pommerening, IMSD, Johannes-Gutenberg-Universitaet,   *
 *   Mainz, 3. April 2001                                      *
 *-------------------------------------------------------------*
 * Version 1.00, 29. Mai 2004                                  *
 ***************************************************************
 */
package de.pseudonymisierung.mainzelliste;

import java.util.Properties;
import java.util.Random;

import org.apache.log4j.Logger;

import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;

/**
 * Here go all the mathematics involved in generating, checking and correcting
 * PIDs. Methods here have package visibility only. A user should call the static
 * methods of class PID.
 *
 * For the theorem, confer publication of Faldum and Pommerening.
 *
 * This class is a C-to-Java-port with generous approval by its author,
 * Klaus Pommerening.
 *
 * @see "Faldum, Andreas and Pommerening, Klaus: An optimal code for patient identifiers. Computer Methods and Programs in Biomedicine 79 (2005), 81–88."
 */
public class PIDGenerator implements IDGenerator<PID>{

	/** The ID type this generator instance produces. */
	private String idType;
	/** The IDGeneratorMemory instance for this generator. */
	private IDGeneratorMemory mem;

	/** Private key for PID generation. */
	@SuppressWarnings("javadoc") // One comment is sufficient, but Eclipse marks a warning otherwise.
	private int key1, key2, key3;
	/** Counter, increased with every created PID. */
	private int counter = 1;
	/** Randomizer instance. */
	private Random rand;
	/**  Internal variable of the algorithm. */
	private static int NN = 1073741824;
	/** 2^30 = module for calc */
	private static int NN_1 = 1073741823;
	/** 2^30 - 1 */
	private static int PIDLength = 8;
	/** Length of a valid PID */
	/**
	 * Least bit for randomization rndfact = 2^(30-rndwidth)
	 */
	private int rndfact;

	/**
	 * Upper limit for random numbers rndlim = 2^(rndwidth)
	 */
	private int rndlim;

	/** Set to 1 after first call to rndsetup */
	private boolean RSET = false;

	/**
	 * alphabet for PIDs
	 *
	 * A codeword is transformed into a PID by replacing each of the
	 * 5-bit-integers by the corresponding character of the alphabet.
	 */
	static char sigma[] = "0123456789ACDEFGHJKLMNPQRTUVWXYZ".toCharArray();

	/** The logging instance. */
	private Logger logger = Logger.getLogger(this.getClass());

	/**
	 * Empty constructor. Needed by IDGeneratorFactory in order to instantiate
	 * an object via reflection.
	 */
	PIDGenerator() {
	}

	/**
	 * Create a PID for the given counter. PIDs are created in a deterministic order.
	 * This method returns the i-th PID of this generator.
	 * @param counter Order of the PID to get.
	 * @return The generated PID string.
	 */
	private String createPIDString(int counter) {
		return PIDgen(counter);
	}

	/**
	 * Check PID. Up to two errors are recognized.
	 *
	 * @param pid The PID to check.
	 * @return true if pid is a correct PID, false otherwise.
	 */
	static boolean isCorrectPID(String pid) {
		if (pid == null || pid.length() != PIDLength)
			return false;

		int codeWord[], sum6, sum7, test6, test7;

		codeWord = PIDGenerator.PID2c(pid);
		sum6 = PIDGenerator.wsum1(codeWord); // checksum 1
		sum7 = PIDGenerator.wsum2(codeWord); // checksum 2
		test6 = sum6 ^ codeWord[6];
		test7 = sum7 ^ codeWord[7];
		if (test6 == 0 && test7 == 0)
			return true;
		else
			return false;
	}

	/**
	 * Internal function used in PID algorithm.
	 */
	@SuppressWarnings("javadoc")
	private static StringBuffer swapPositions(StringBuffer str, int pos1,
			int pos2) {
		StringBuffer ret = new StringBuffer(str);
		ret.setCharAt(pos1, str.charAt(pos2));
		ret.setCharAt(pos2, str.charAt(pos1));
		return ret;
	}

	/**
	 * Tries to correct a PID.
	 *
	 * Up to two errors are recognized, errors with one changed character or a
	 * transposition of two adjacent characters can be corrected.
	 *
	 * @param PIDString
	 *            The PID to correct.
	 * @return PIDString, if it is a correct PID, the corrected PID if PIDString
	 *         is invalid but can be corrected, null if PIDString is invalid and
	 *         cannot be corrected.
	 */
	static String correctPID(String PIDString) {

		if (PIDString == null || PIDString.length() != PIDLength)
			return null;

		int c[] = PID2c(PIDString);

		int sum6, sum7, test6, test7;

		StringBuffer PIDStringBuffer = new StringBuffer(PIDString);

		if (c == null)
			return null;

		sum6 = wsum1(c); /* checksum 1 */
		sum7 = wsum2(c); /* checksum 2 */
		test6 = sum6 ^ c[6];
		test7 = sum7 ^ c[7];

		if (test6 == 0) { /* checksum 1 correct */
			if (test7 == 0)
				return PIDString; /* checksum 2 correct */
			else {
				PIDStringBuffer.setCharAt(7, sigma[sum7]); /*
															 * correct checksum
															 * 2
															 */
			}
		} else {
			if (test7 == 0) { /* checksum 2 correct */
				PIDStringBuffer.setCharAt(6, sigma[sum6]); /*
															 * correct checksum
															 * 1
															 */
			} else {
				if (test7 == multf32(test6, 1)) { /* c[0] wrong */
					PIDStringBuffer.setCharAt(0, sigma[c[0]
							^ multf32(test6, 30)]); /* correct char */
				} else if (test7 == multf32(test6, 2)) { /* c[1] wrong */
					PIDStringBuffer.setCharAt(1, sigma[c[1]
							^ multf32(test6, 29)]); /* correct char */
				} else if (test7 == multf32(test6, 3)) { /* c[2] wrong */
					PIDStringBuffer.setCharAt(2, sigma[c[2]
							^ multf32(test6, 28)]); /* correct char */
				} else if (test7 == multf32(test6, 4)) { /* c[3] wrong */
					PIDStringBuffer.setCharAt(3, sigma[c[3]
							^ multf32(test6, 27)]); /* correct char */
				} else if (test7 == multf32(test6, 5)) { /* c[4] wrong */
					PIDStringBuffer.setCharAt(4, sigma[c[4]
							^ multf32(test6, 26)]); /* correct char */
				} else if (test7 == multf32(test6, 6)) { /* c[5] wrong */
					PIDStringBuffer.setCharAt(5, sigma[c[5]
							^ multf32(test6, 25)]); /* correct char */
				} else if (test7 == test6 && /* corrected 1.Oct.2007 */
				test7 == (c[6] ^ c[7])) { /* c[6], c[7] interchanged */
					PIDStringBuffer = swapPositions(PIDStringBuffer, 6, 7);
				} else if (test7 == multf32(test6, 16) && /* corrected 1.Oct.2007 */
				(multf32(c[5], 6) ^ multf32(c[6], 6) ^ c[5] ^ c[6]) == test6) { //c [5], c[6] int'd
					PIDStringBuffer = swapPositions(PIDStringBuffer, 5, 6);
				} else if (test7 == multf32(test6, 19) && /* corrected 1.Oct.2007 */
				multf32(c[0] ^ c[1], 19) == test6) { /* c[0],c[1] int'd */
					PIDStringBuffer = swapPositions(PIDStringBuffer, 0, 1);
				} else if (test7 == multf32(test6, 20) && /* corrected 1.Oct.2007 */
				multf32(c[1] ^ c[2], 20) == test6) { /* c[1],c[2] int'd */
					PIDStringBuffer = swapPositions(PIDStringBuffer, 1, 2);
				} else if (test7 == multf32(test6, 21) && /* corrected 1.Oct.2007 */
				multf32(c[2] ^ c[3], 21) == test6) { /* c[2],c[3] int'd */
					PIDStringBuffer = swapPositions(PIDStringBuffer, 2, 3);
				} else if (test7 == multf32(test6, 22) && /* corrected 1.Oct.2007 */
				multf32(c[3] ^ c[4], 22) == test6) { /* c[3],c[4] int'd */
					PIDStringBuffer = swapPositions(PIDStringBuffer, 3, 4);
				} else if (test7 == multf32(test6, 23) && /* corrected 1.Oct.2007 */
				multf32(c[4] ^ c[5], 23) == test6) { /* c[4],c[5] int'd */
					PIDStringBuffer = swapPositions(PIDStringBuffer, 4, 5);
				} else
					return null; /* at least 2 characters wrong */
				/* and no simple interchange */
			}
		}
		return PIDStringBuffer.toString();
	}

	/**
	 * Initialize random generator. Set global variables rndfact, rndlim. Must
	 * be called before first use of rndext.
	 *
	 * Error handling: Values > 12 of rndwith are treated as = 12
	 *
	 * @param rndwidth
	 *            The desired random width.
	 */
	private void rndsetup(int rndwidth) {
		if (RSET)
			return;

		RSET = true;
		rand = new Random();
		rand.setSeed(System.currentTimeMillis()); /* set seed */

		if (rndwidth > 12)
			rndwidth = 12;
		rndfact = 1 << (30 - rndwidth);
		rndlim = 1 << rndwidth;
	}

	/**
	 * Randomize a number
	 *
	 * Replace first rndwidth bits of x with random bits
	 *
	 * Error handling: If x >= rndfact, overflowing bits are dropped.
	 *
	 * Used in encr
	 *
	 * @param x
	 *            Integer, 0 <= x < rndfact.
	 * @return x with first rndwidth bits of x replaced by random bits
	 */
	private int rndext(int x) {
		int r;
		int rr;
		double r1;

		x = x & (rndfact - 1);
		r1 = rand.nextDouble(); /* 0 <= r1 < 1 */
		rr = (int) (rndlim * r1);
		r = rr * rndfact + x;
		return r;
	}

	/**
	 * Multiply x and y mod 2^30 used in encr.
	 */
	@SuppressWarnings("javadoc")
	private static int mult30(int x, int y) {
		int z;
		z = x * y; /* multiply, dropping long int overflow */
		z = z & NN_1; /* reduce mod 2^30 */
		return z;
	}

	/**
	 * Rotate x cyclically by 6 bits to the right. Bit 29 becomes bit 23, ...,
	 * bit 6 becomes bit 0, bit 5 becomes bit 29, ..., bit 0 becomes bit 24.
	 * Error handling: If x is not in the required range, overflowing bits are
	 * dropped. Used in encr
	 */
	@SuppressWarnings("javadoc")
	static int rot30_6(int x) {
		int y, z;
		y = x & 63; /* preserve last 6 bits */
		y = y << 24; /* shift them to the left */
		z = x >> 6; /* remaining bits to the right */
		z = z & 16777215; /* clear overflowing bits */
		/* 16777215 = 2^24 - 1 */
		z = z | y; /* prefix preserved 6 bits */
		return z;
	}

	/**
	 * Nonlinear transform of x. Split the input x into five 6-bit-chunks
	 * [e|d|c|b|a], replace a with the quadratic expression a + b*e + c*d mod
	 * 2^6. Because of an error the transformation is a + b*d + c*d mod 2^6
	 * instead. This transformation is bijective on 30-bit-integers. Error
	 * handlicng: If x is not in the required range, overflowing bits are
	 * dropped. Used in encr
	 */
	@SuppressWarnings("javadoc")
	private static int NLmix(int x) {
		int y;
		int a, b, c, d, e;
		a = x & 63; /* extract last 6 bits */
		y = x >> 6; /* shift remaining bits to the right */
		b = y & 63; /* extract last 6 bits */
		y = y >> 6; /* shift remaining bits to the right */
		c = y & 63; /* extract last 6 bits */
		y = y >> 6; /* shift remaining bits to the right */
		d = y & 63; /* extract last 6 bits */
		e = y >> 6; /* get remaining 6 bits */
		/*** This should have been y = y >> 6. This error ***/
		/*** must NEVER be corrected. It changes the intended ***/
		/*** cryptographic transformation into another one ***/
		/*** that is perfectly valid for its own, but probably ***/
		/*** somewhat weaker. ***/
		e = y & 63; /* clear overflowing bits */

		a = (a + b * e + c * d) & 63; /* quadratic expression mod 2^6 */
		y = x & 1073741760; /* AND 2^30 - 2^6 = */
		/* preserve bits 6 to 29 */
		y = y | a; /* and append new 6 bits */
		return y;
	}

	/**
	 * Permutation of the bits of x.
	 *
	 * Split the input x into six 5-bit-chunks, permute each chunk with the same
	 * fixed permutation This transformation is bijective on 30-bit-integers.
	 * Error handling: If x is not in the required range, overflowing bits are
	 * dropped. Used in encr.
	 *
	 * @param x
	 *            The value which to permute, assumed to be an unsigned 30-bit
	 *            Integer.
	 * @return The permuted value.
	 */
	private static int bitmix(int x) {
		int p[] = new int[5];
		int xx[] = new int[5];
		int yy[] = new int[5];
		int y;
		int i;

		p[0] = 34636833; /* 2^25 + 2^20 + 2^15 + 2^10 + 2^5 + 2^0 */
		for (i = 1; i <= 4; i++)
			p[i] = p[i - 1] << 1;
		for (i = 0; i <= 4; i++)
			xx[i] = x & p[i]; /* every 5th bit */
		yy[0] = xx[3] >> 3; /* permute */
		yy[1] = xx[0] << 1;
		yy[2] = xx[4] >> 2;
		yy[3] = xx[2] << 1;
		yy[4] = xx[1] << 3;
		y = yy[0] | yy[1] | yy[2] | yy[3] | yy[4]; /* and glue */

		return y;
	}

	/**
	 * Encrypt x with keys k1, k2, k3, and k4 = k1+k2+k3 mod 2^30.
	 *
	 * The encryption consists of 4 rounds; each round consists of:
	 * <ul>
	 * <li>first rot30_6,
	 * <li>then NLmix,
	 * <li>finally multiply with ki mod 2^30.
	 * </ul>
	 * Between rounds 2 and 3 apply bitmix. This transformation is bijective on
	 * 30-bit-integers, if all ki are odd.
	 *
	 * Error handling: If ki is even, it's replaced with ki + 1. If x is not in
	 * the required range, overflowing bits are dropped.
	 *
	 * Used in PIDgen
	 *
	 * @param x
	 *            The value to encrypt. Assumed to be an unsigned
	 *            30-bit-Integer.
	 * @return The encrypted value, in the range of an unsigned 30-bit-Integer.
	 */
	private int encr(int x) {
		int w, y, z, k1, k2, k3, k4;
		k1 = this.key1 | 1; /* k1 may be even - make it odd */
		k2 = this.key2 | 1; /* k2 may be even - make it odd */
		k3 = this.key3 | 1; /* k3 may be even - make it odd */
		k4 = (k1 + k2 + k3) & NN_1; /* Key for round 4 */

		y = rot30_6(x); /* round 1 */
		w = NLmix(y);
		z = mult30(k1, w);
		y = rot30_6(z); /* round 2 */
		w = NLmix(y);
		z = mult30(k2, w);
		w = bitmix(z); /* permutation */
		y = rot30_6(z); /* round 3 */
		w = NLmix(y);
		z = mult30(k3, w);
		y = rot30_6(z); /* round 4 */
		w = NLmix(y);
		z = mult30(k4, w);
		return z;

	}

	/**
	 * Split a 30-bit integer x into an array p of six 5-bit-integers.
	 *
	 * Error handling: If x is not in the required range, overflowing bits are
	 * dropped. Used in PIDgen
	 */
	@SuppressWarnings("javadoc")
	private static int[] u2pcw(int x) {
		int y;
		int p[] = new int[6];

		p[5] = x & 31; /* extract last 5 bits */
		y = x >> 5; /* shift remaining bit to the right */
		p[4] = y & 31; /* extraxt last 5 bits */
		y = y >> 5; /* shift remaining bit to the right */
		p[3] = y & 31; /* extraxt last 5 bits */
		y = y >> 5; /* shift remaining bit to the right */
		p[2] = y & 31; /* extraxt last 5 bits */
		y = y >> 5; /* shift remaining bit to the right */
		p[1] = y & 31; /* extraxt last 5 bits */
		y = y >> 5; /* shift remaining bit to the right */
		p[0] = y & 31; /* extraxt last 5 bits */

		return p;
	}

	/**
	 * Arithmetic in the Galois field F_32.
	 *
	 * t is a primitive element with t^5 = t^2 + 1. Multiply the 5 bit input x =
	 * (x4, x3, x2, x1, x0) with t^e where 0 <= e <= 3; the algorithm is
	 * described in the documentation.
	 *
	 * Error handling: If x has more then 5 bits, overflowing bits are dropped.
	 * If e is < 0 or > 3, it's treated as 0, i. e. the function returns x.
	 *
	 * Used in multf32
	 */
	@SuppressWarnings("javadoc")
	static int mult0f32(int x, int e) {
		int u, v, w, s;
		x = x & 31; /* drop overflowing bits */
		if (e == 0)
			return x; /* catch trivial case and */
		if (e > 3)
			return x; /* unwanted values */

		u = x >> (5 - e);
		v = u << 2;
		w = (x << e) & 31;
		s = (u ^ v) ^ w;
		return s;
	}

	/**
	 * Omitted from original source code: wsum (Was not used).
	 */

	/**
	 * Transform to codeword.
	 *
	 * Transform an array p of six 5-bit-integers into a codeword, consisting of
	 * eight 5-bit-integers. Used in PIDgen.
	 *
	 * @param p
	 *            Array of six Integers, assumed to be unsigned 5-bit.
	 * @return Array of eight Integers in the range of unsigned 5-bit-Numbers.
	 */
	private static int[] encode(int p[]) {
		int c[] = new int[8];
		int i;
		for (i = 0; i <= 5; i++)
			c[i] = p[i]; /* preserve input elements */
		c[6] = PIDGenerator.wsum1(p); /* weighted sum in F_32 */
		c[7] = PIDGenerator.wsum2(p); /* weighted sum in F_32 */

		return c;
	}

	/**
	 * Generate PID.
	 *
	 */
	@SuppressWarnings("javadoc")
	private String PIDgen(int x) {
		int y, z;
		int j; /* loop counter */
		int pcw[]; /* intermediate result */
		int cw[]; /* intermediate result */
		char p[] = "00000000".toCharArray(); /* output variable */

		if (x == 0)
			return new String(p); /* x outside required range */
		if (x >= NN)
			return new String(p); /* x outside required range */

		// rndsetup(rndwidth); // moved to constructor
		y = rndext(x); /* randomize (or not) */
		z = encr(y); /* encrypt */
		pcw = u2pcw(z); /* split */
		cw = encode(pcw); /* encode */

		for (j = 0; j <= 7; j++)
			p[j] = PIDGenerator.sigma[cw[j]]; /* rewrite as PID */
		return new String(p);
	}

	/**
	 * Transform a string s into a codeword c.
	 *
	 * If s has length != 8 or contains a character not in the alphabet sigma,
	 * the function returns 0 and the output variable c is meaningless. Note:
	 * Lowercase letters are converted to uppercase. Otherwise c is the codeword
	 * corresponding to s and the function returns 1. ---> used in PIDcheck
	 */
	@SuppressWarnings("javadoc")
	static int[] PID2c(String s) {
		if (s.length() != 8)
			return null;
		int i;
		char p[] = s.toCharArray();
		int c[] = new int[8];

		for (i = 0; i < 8; i++)
			c[i] = 0;
		for (i = 0; i < 8; i++) { /* convert chars to 5-bit-integers */
			switch (p[i]) {
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				c[i] = p[i] - '0';
				break;
			case 'A':
			case 'a':
				c[i] = 10;
				break;
			case 'C':
			case 'D':
			case 'E':
			case 'F':
			case 'G':
			case 'H':
				c[i] = p[i] - 'C' + 11;
				break;
			case 'c':
			case 'd':
			case 'e':
			case 'f':
			case 'g':
			case 'h':
				c[i] = p[i] - 'c' + 11;
				break;
			case 'J':
			case 'K':
			case 'L':
			case 'M':
			case 'N':
				c[i] = p[i] - 'J' + 17;
				break;
			case 'j':
			case 'k':
			case 'l':
			case 'm':
			case 'n':
				c[i] = p[i] - 'j' + 17;
				break;
			case 'P':
			case 'Q':
			case 'R':
				c[i] = p[i] - 'P' + 22;
				break;
			case 'p':
			case 'q':
			case 'r':
				c[i] = p[i] - 'p' + 22;
				break;
			case 'T':
			case 'U':
			case 'V':
			case 'W':
			case 'X':
			case 'Y':
			case 'Z':
				c[i] = p[i] - 'T' + 25;
				break;
			/*** missing break inserted 17. April 2005 KP ***/
			case 't':
			case 'u':
			case 'v':
			case 'w':
			case 'x':
			case 'y':
			case 'z':
				c[i] = p[i] - 't' + 25;
				break;
			default: /* invalid character found */
				return null;
			}
		}
		return c;
	}

	/**
	 * Output weighted sum.
	 *
	 * Output the weighted sum t p[0] + t^2 p[1] + t^3 p[2] + t^4 p[3] + t^5
	 * p[4] + t^6 p[5] in F_32. Used in encode and PIDcheck.
	 */
	@SuppressWarnings("javadoc")
	static int wsum1(int p[]) {
		int s;
		int i;
		s = 0;
		for (i = 0; i <= 5; i++)
			s = s ^ PIDGenerator.multf32(p[i], i + 1);
		return s;
	}

	/**
	 * Output weighted sum.
	 *
	 * Output the weighted sum t^2 p[0] + t^4 p[1] + t^6 p[2] + t^8 p[3] + t^10
	 * p[4] + t^12 p[5] in F_32. Used in encode and PIDcheck.
	 */
	@SuppressWarnings("javadoc")
	static int wsum2(int p[]) {
		int s;
		int i;
		s = 0;
		for (i = 0; i <= 5; i++)
			s = s ^ PIDGenerator.multf32(p[i], 2 * i + 2);
		return s;
	}

	/**
	 * Arithmetic in the Galois field F_32.
	 *
	 * t is a primitive element with t^5 = t^2 + 1. Multiply the 5 bit input x =
	 * (x4, x3, x2, x1, x0) with t^e where e is an unsigned integer. Error
	 * handling: If x has more then 5 bits, overflowing bits are dropped.
	 *
	 * Used in wsum1, wsum2, and PIDcheck.
	 */
	@SuppressWarnings("javadoc")
	static int multf32(int x, int e) {
		x = x & 31; /* drop overflowing bits */
		while (e >= 4) {
			x = mult0f32(mult0f32(x, 2), 2); /* multiply by t^4 */
			e = e - 4;
		}
		x = mult0f32(x, e);
		return x;
	}

	@Override
	public void init(IDGeneratorMemory mem, String idType, Properties props) {
		this.mem = mem;

		String memCounter = mem.get("counter");
		if(memCounter == null) memCounter = "0";
		this.counter = Integer.parseInt(memCounter);

		this.idType = idType;

		try {
			int key1 = Integer.parseInt(props.getProperty("k1"));
			int key2 = Integer.parseInt(props.getProperty("k2"));
			int key3 = Integer.parseInt(props.getProperty("k3"));
			int rndwidth;
			if (props.containsKey("rndwidth"))
				rndwidth = Integer.parseInt(props.getProperty("rndwidth"));
			else
				rndwidth = 0;
			this.key1 = key1;
			this.key2 = key2;
			this.key3 = key3;
			this.rndsetup(rndwidth);
		} catch (NumberFormatException e) {
			logger.fatal("Number format error in configuration of IDGenerator for ID type " + idType, e);
			throw new InternalErrorException(e);
		}
	}

	@Override
	public synchronized PID getNext() {
		String pid = createPIDString(this.counter + 1);
		this.counter++;
		mem.set("counter", Integer.toString(this.counter));
		mem.commit();
		return new PID(pid, idType);
	}

	@Override
	public boolean verify(String id) {
		return isCorrectPID(id);
	}

	@Override
	public String correct(String PIDString) {
		return correctPID(PIDString);
	}

	@Override
	public PID buildId(String id) {
		return new PID(id, getIdType());
	}

	@Override
	public String getIdType() {
		return idType;
	}

	@Override
	public boolean isExternal() { return false; }
}
