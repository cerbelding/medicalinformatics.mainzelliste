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
