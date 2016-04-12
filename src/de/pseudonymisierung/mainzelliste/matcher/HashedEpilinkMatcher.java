package de.pseudonymisierung.mainzelliste.matcher;

import java.util.Properties;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;
import de.pseudonymisierung.mainzelliste.matcher.hasher.Hasher;

/**
 * Represent a Matcher for record linkage. The calculation is based on the
 * EpilinkMatcher. Whether two patients are compared by their attributes,
 * depends on the comparison of their hashes.
 * 
 * For using this Matcher, a threshold must be specified in the configuration
 * file. If no Hasher is specified, the behaviour is like the EpilinkMatcher.
 */
public class HashedEpilinkMatcher extends EpilinkMatcher
{
    /** Specified Threshold */
    private int thresholdHashes;
    
    /** Hasher which is used for a comparison of two hashes */
    private Hasher comparatorHash;
    
    /**
	 * Returns the specified threshold.
     *
     * @return Specified threshold
     */
    public int getThresholdHash()
    {
        return thresholdHashes;
    }
    
    /**
	 * Returns the instance of the used Hasher. If no Hasher is specified,
	 * NULL is returned.
     * 
     * @return Instance of the Hasher or NULL, if no Hasher is specified.
     */
    public Hasher getHasher()
    {
        return comparatorHash;
    }
    
    @Override
    public void initialize(Properties props) throws InternalErrorException
    {
        super.initialize(props);
        
        String thresholdHashesProperty = props.getProperty("Hash.threshold");
        thresholdHashes = Integer.parseInt((thresholdHashesProperty != null) ? thresholdHashesProperty : "-1000");
        
        String hashUseProperty = props.getProperty("Hasher.use");
        if (hashUseProperty != null && hashUseProperty.equals("true"))
        {
            try
            {
                Class<?> hasherClass = Class.forName("de.pseudonymisierung.mainzelliste.matcher.hasher." + props.getProperty("Hash.hasher"));
                comparatorHash = (Hasher) hasherClass.newInstance();
            }
            catch (Exception e)
            {
                throw new InternalErrorException();
            }
        }
    }

    @Override
    public MatchResult match(Patient a, Iterable<Patient> patientList)
    {
        if (comparatorHash == null)
            return super.match(a, patientList);
        
        Patient bestMatch = null;
        double bestWeight = Double.NEGATIVE_INFINITY;

        for (Patient b : patientList)
        {
            int comparedHashes = comparatorHash.compare(a.getHash(), b.getHash());

            if (comparedHashes >= thresholdHashes)
            {
                // assert that the persons have the same Fields 
                assert (a.getFields().keySet().equals(b.getFields().keySet()));
                double weight = calculateWeight(a, b);
                
                if (weight > bestWeight)
                {
                    bestWeight = weight;
                    bestMatch = b;
                }
            }
        }

        if (bestWeight >= getThresholdMatch())
        {
            return new MatchResult(MatchResultType.MATCH, bestMatch, bestWeight);
        }
        else if (bestWeight < getThresholdMatch() && bestWeight > getThresholdNonMatch())
        {
            return new MatchResult(MatchResultType.POSSIBLE_MATCH, bestMatch, bestWeight);
        }
        else
        {
            return new MatchResult(MatchResultType.NON_MATCH, null, bestWeight);
        }
    }
}
