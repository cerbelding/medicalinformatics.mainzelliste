package de.securerecordlinkage.helperClasses;

import java.util.HashMap;
import java.util.Map;

//TODO: should not necessarily be a complete implementation, because the code is identical to MatchCounter.
public class TentativeMatchCounter {

    private static Map<String, Integer> numAll = new HashMap<>();
    private static Map<String, Integer> numMatch = new HashMap<>();
    private static Map<String, Integer> numNonMatch = new HashMap<>();

    public static Integer getNumAll(String remoteID) {
        return TentativeMatchCounter.numAll.get(remoteID);
    }

    public static void setNumAll(String remoteID, Integer numAll) {
        TentativeMatchCounter.numAll.put(remoteID, numAll);
    }

    public static Integer getNumMatch(String remoteID) {
        return TentativeMatchCounter.numMatch.get(remoteID);
    }

    public static void setNumMatch(String remoteID, Integer numMatch) {
        TentativeMatchCounter.numMatch.put(remoteID, numMatch);
    }

    public static void incrementNumMatch(String remoteID) {
        if (!TentativeMatchCounter.numMatch.containsKey(remoteID)) {
            TentativeMatchCounter.numMatch.put(remoteID, 0);
        }
        ;
        Integer oldValue = TentativeMatchCounter.numMatch.get(remoteID);
        Integer newValue = oldValue + 1;
        TentativeMatchCounter.numMatch.put(remoteID, newValue);
    }

    public static void incrementNumNonMatch(String remoteID) {
        if (!TentativeMatchCounter.numNonMatch.containsKey(remoteID)) {
            TentativeMatchCounter.numNonMatch.put(remoteID, 0);
        }
        ;
        Integer oldValue = TentativeMatchCounter.numNonMatch.get(remoteID);
        Integer newValue = oldValue + 1;
        TentativeMatchCounter.numNonMatch.put(remoteID, newValue);
    }

    public static Integer getNumNonMatch(String remoteID) {
        return TentativeMatchCounter.numNonMatch.get(remoteID);
    }

    public static void setNumNonMatch(String remoteID, Integer numNonMatch) {
        TentativeMatchCounter.numNonMatch.put(remoteID, numNonMatch);
    }

}
