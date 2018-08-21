package de.securerecordlinkage;

import java.util.HashMap;
import java.util.Map;

public class MatchCounter {

    private static Map<String, Integer> numAll = new HashMap<>();
    private static Map<String, Integer> numMatch = new HashMap<>();
    private static Map<String, Integer> numNonMatch = new HashMap<>();

    public static Integer getNumAll(String remoteID) {
        return MatchCounter.numAll.get(remoteID);
    }

    public static void setNumAll(String remoteID, Integer numAll) {
        MatchCounter.numAll.put(remoteID, numAll);
    }

    public static Integer getNumMatch(String remoteID) {
        return MatchCounter.numMatch.get(remoteID);
    }

    public static void setNumMatch(String remoteID, Integer numMatch) {
        MatchCounter.numMatch.put(remoteID, numMatch);
    }

    public static void incrementNumMatch(String remoteID) {
        if (!MatchCounter.numMatch.containsKey(remoteID)) {
            MatchCounter.numMatch.put(remoteID, 0);
        }
        ;
        Integer oldValue = MatchCounter.numMatch.get(remoteID);
        Integer newValue = oldValue + 1;
        MatchCounter.numMatch.put(remoteID, newValue);
    }

    public static Integer getNumNonMatch(String remoteID) {
        return MatchCounter.numNonMatch.get(remoteID);
    }

    public static void setNumNonMatch(String remoteID, Integer numNonMatch) {
        MatchCounter.numNonMatch.put(remoteID, numNonMatch);
    }

}
