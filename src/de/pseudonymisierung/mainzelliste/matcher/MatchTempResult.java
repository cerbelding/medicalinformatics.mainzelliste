package de.pseudonymisierung.mainzelliste.matcher;

import de.pseudonymisierung.mainzelliste.Patient;

import java.util.List;
import java.util.TreeMap;

public class MatchTempResult {

    double bestWeight = Double.NEGATIVE_INFINITY;
    TreeMap<Double, List<Patient>> possibleMatches = new TreeMap<Double, List<Patient>>();
    Patient bestMatch = null;

    public Patient getBestMatch() {
        return bestMatch;
    }

    public void setBestMatch(Patient bestMatch) {
        this.bestMatch = bestMatch;
    }

    public double getBestWeight() {
        return bestWeight;
    }

    public void setBestWeight(double bestWeight) {
        this.bestWeight = bestWeight;
    }

    public TreeMap<Double, List<Patient>> getPossibleMatches() {
        return possibleMatches;
    }

    public void setPossibleMatches(TreeMap<Double, List<Patient>> possibleMatches) {
        this.possibleMatches = possibleMatches;
    }


}
