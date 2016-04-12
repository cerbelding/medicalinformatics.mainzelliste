package de.pseudonymisierung.mainzelliste.matcher.hasher;

import java.util.*;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Computes the Nilsimsa hash for the given string.
 *
 * @author Albert Weichselbraun <albert.weichselbraun@htwchur.ch>
 * <weichselbraun@weblyzard.com>
 *
 * This class is a translation of the Python implementation by Michael Itz to
 * the Java language <http://code.google.com/p/py-nilsimsa>.
 *
 * Original C nilsimsa-0.2.4 implementation by cmeclax:
 * <http://ixazon.dynip.com/~cmeclax/nilsimsa.html>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; version 3 dated June, 2007.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */
public class Nilsimsa
{
    private int count = 0; 		  		// num characters seen
    private int[] acc = new int[256]; 	// accumulators for computing the digest
    private int[] lastch = new int[4];	// the last four seen characters

    // pre-defined transformation arrays
    private static final byte[] TRAN = Nilsimsa._getByteArray(
            "02D69E6FF91D04ABD022161FD873A1AC"
            + "3B7062961E6E8F399D05144AA6BEAE0E"
            + "CFB99C9AC76813E12DA4EB518D646B50"
            + "23800341ECBB71CC7A867F98F2365EEE"
            + "8ECE4FB832B65F59DC1B314C7BF06301"
            + "6CBA07E81277493CDA46FE2F791C9B30"
            + "E300067E2E0F383321ADA554CAA729FC"
            + "5A47697DC595B5F40B90A3816D255535"
            + "F575740A26BF195C1AC6FF995D84AA66"
            + "3EAF78B32043C1ED24EAE63F18F3A042"
            + "57085360C3C0834082D709BD442A67A8"
            + "93E0C2569FD9DD8515B48A27289276DE"
            + "EFF8B2B7C93D45944B110D65D5348B91"
            + "0CFA87E97C5BB14DE5D4CB10A21789BC"
            + "DBB0E2978852F748D3612C3A2BD18CFB"
            + "F1CDE46AE7A9FDC437C8D2F6DF58724E");

	// pre-defined array for the computation of the bitwise difference
    // between two nilsimsa strings.
    private static final byte[] POPC = Nilsimsa._getByteArray(
            "00010102010202030102020302030304"
            + "01020203020303040203030403040405"
            + "01020203020303040203030403040405"
            + "02030304030404050304040504050506"
            + "01020203020303040203030403040405"
            + "02030304030404050304040504050506"
            + "02030304030404050304040504050506"
            + "03040405040505060405050605060607"
            + "01020203020303040203030403040405"
            + "02030304030404050304040504050506"
            + "02030304030404050304040504050506"
            + "03040405040505060405050605060607"
            + "02030304030404050304040504050506"
            + "03040405040505060405050605060607"
            + "03040405040505060405050605060607"
            + "04050506050606070506060706070708");

    public Nilsimsa()
    {
        reset();
    }

    /**
     * Updates the Nilsimsa digest using the given String
     *
     * @param s: the String data to consider in the update
     */
    public void update(String s)
    {
        for (int ch : s.toCharArray())
        {
            count++;

            // incr accumulators for triplets
            if (lastch[1] > -1)
            {
                acc[_tran3(ch, lastch[0], lastch[1], 0)]++;
            }
            if (lastch[2] > -1)
            {
                acc[_tran3(ch, lastch[0], lastch[2], 1)]++;
                acc[_tran3(ch, lastch[1], lastch[2], 2)]++;
            }
            if (lastch[3] > -1)
            {
                acc[_tran3(ch, lastch[0], lastch[3], 3)]++;
                acc[_tran3(ch, lastch[1], lastch[3], 4)]++;
                acc[_tran3(ch, lastch[2], lastch[3], 5)]++;
                acc[_tran3(lastch[3], lastch[0], ch, 6)]++;
                acc[_tran3(lastch[3], lastch[2], ch, 7)]++;
            }

            // adjust lastch
            for (int i = 3; i > 0; i--)
            {
                lastch[i] = lastch[i - 1];
            }
            lastch[0] = ch;
        }
    }

    /**
     * resets the Hash computation
     */
    private void reset()
    {
        count = 0;
        Arrays.fill(acc, (byte) 0);
        Arrays.fill(lastch, -1);
    }

    /**
     * Converts the given hexString to a byte array. 
     * @param hexString: the hexString to convert
     * @return the corresponding byte array
     */
    private static byte[] _getByteArray(String hexString)
    {
        try
        {
            return Hex.decodeHex(hexString.toCharArray());
        }
        catch (DecoderException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Accumulator for a transition n between the chars a, b, c
     */
    private int _tran3(int a, int b, int c, int n)
    {
        int i = (c) ^ TRAN[n];
        return (((TRAN[(a + n) & 255] ^ TRAN[b & 0xff] * (n + n + 1)) + TRAN[i & 0xff]) & 255);
    }

    /**
     * @return the digest for the current Nilsimsa object.
     */
    public byte[] digest()
    {
        int total = 0;
        int threshold;
        byte[] digest = new byte[32];
        Arrays.fill(digest, (byte) 0);

        if (count == 3)
        {
            total = 1;
        }
        else if (count == 4)
        {
            total = 4;
        }
        else if (count > 4)
        {
            total = 8 * count - 28;
        }
        threshold = total / 256;

        for (int i = 0; i < 256; i++)
        {
            if (acc[i] > threshold)
            {
                digest[i >> 3] += 1 << (i & 7);
            }
        }
        ArrayUtils.reverse(digest);
        return digest;
    }

    /**
     * @return a String representation of the current state of the Nilsimsa
     * object.
     */
    public String hexdigest()
    {
        return Hex.encodeHexString(digest());
    }

    /**
     * Compute the Nilsimsa digest for the given String.
     *
     * @param s: the String to hash
     * @return the Nilsimsa digest.
     */
    public byte[] digest(String s)
    {
        reset();
        update(s);
        return digest();
    }

    /**
     * Compute the Nilsimsa hexDigest for the given String.
     *
     * @param s: the String to hash
     * @return the Nilsimsa hexdigest.
     */
    public String hexdigest(String s)
    {
        return Hex.encodeHexString(digest(s));
    }

    /**
     * Compares a Nilsimsa object to the current one and return the number of
     * bits that differ.
     *
     * @param cmp: the comparison object
     * @return the number of bits the strings differ.
     */
    public int compare(Nilsimsa cmp)
    {
        int bits = 0;
        int j;
        byte[] n1 = digest();
        byte[] n2 = cmp.digest();

        for (int i = 0; i < 32; i++) {
            j = 255 & (n1[i] ^ n2[i]);
            bits += POPC[j];
        }
        return 128 - bits;
    }
}
