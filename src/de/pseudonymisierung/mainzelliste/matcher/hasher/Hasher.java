package de.pseudonymisierung.mainzelliste.matcher.hasher;

/**
 * Interface of a Hasher.
 * 
 * @author Christopher Hampf
 */
public interface Hasher
{
    /**
     * The respective implementation generates a hash on the basis of the
     * implemented algorithm.
     * 
     * @param input input data for hash generating
     * 
     * @return Generated hash
     */
    public String generate(String input);
    
    /**
     * The respective implementation compares two given strings and
     * returns a score depends on the implemented algorithm. The score
     * represent the similarity of both hashes.
     * 
     * @param hash1 First hash
     * @param hash2 Second hash
     * 
     * @return Score which represent the similarity of both hashes.
     */
    public int compare(String hash1, String hash2);
}
