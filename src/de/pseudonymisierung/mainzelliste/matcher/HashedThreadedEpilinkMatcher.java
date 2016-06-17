package de.pseudonymisierung.mainzelliste.matcher;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;

/**
 * Represents a multithreaded version of the HashedEpilinkMatcher. The
 * calculation is based on the HashedEpilinkMatcher. Whether two patients are
 * compared by their attributes, depends on the comparison of their hashes.
 *
 * For using this Matcher, a threshold must be specified in the configuration
 * file. If no Hasher is specified, the behaviour is like the EpilinkMatcher.
 */
public class HashedThreadedEpilinkMatcher extends HashedEpilinkMatcher {

    /**
     * Container for the best MatchResult. A single instance of this class is
     * accessed by all workers and updated whenever a better match is found.
     */
    private class MatchResultContainer {

        @SuppressWarnings("javadoc")
        public MatchResult matchResult;
    }

    /**
     * Thread that matches on a subset of patients.
     */
    private class MatchCallable implements Runnable {

        /**
         * The patients to match against. Acts as a queue from which the workers
         * draw patients to process.
         */
        private Iterator<Patient> iterator;
        /**
         * The (new) patient which to match to the patient list.
         */
        private Patient left;
        /**
         * The best match this worker finds.
         */
        private MatchResultContainer bestMatchResult;

        //private long[] times;
        /**
         * Create an instance.
         *
         * @param left The (new) patient which to compare against the others.
         * @param iterator Queue from which to draw patients to match against.
         * @param bestMatchResult Object to store the best match result in.
         */
        public MatchCallable(Patient left, Iterator<Patient> iterator, MatchResultContainer bestMatchResult/*, long[] resultTimes*/) {
            this.iterator = iterator;
            this.left = left;
            this.bestMatchResult = bestMatchResult;
            //this.times = resultTimes;
        }

        /**
         * Draws patient from the queue provided in the constructor and compares
         * the new patient against them until no more patients are in the queue.
         */
        @Override
        public void run() {
            Patient right;

            do {
                synchronized (iterator) {
                    if (!iterator.hasNext()) {
                        break;
                    }
                    right = iterator.next();
                }

                double thisWeight = 0.0;

                int comparedHashes = getHasher().compare(left.getHash(), right.getHash());
                if (comparedHashes >= getThresholdHash()) {
                    thisWeight = calculateWeight(left, right);
                }

                synchronized (bestMatchResult) {
                    if (thisWeight > bestMatchResult.matchResult.getBestMatchedWeight()) {
                        MatchResultType t;
                        if (thisWeight >= getThresholdMatch()) {
                            t = MatchResultType.MATCH;
                        } else if (thisWeight >= getThresholdNonMatch()) {
                            t = MatchResultType.POSSIBLE_MATCH;
                        } else {
                            t = MatchResultType.NON_MATCH;
                        }
                        bestMatchResult.matchResult = new MatchResult(t, right, thisWeight);
                    }
                }
            } while (true);
        }
    }

    //private long[] times;
    /**
     * Puts all patients to match against in a queue and creates a set of
     * workers to process the queue.
     *
     * @see MatchCallable
     */
    @Override
    public MatchResult match(Patient a, Iterable<Patient> patientList) {

        if (getHasher() == null) {
            return super.match(a, patientList);
        }

        MatchResult bestMatchResult = new MatchResult(MatchResultType.NON_MATCH, null, -Double.MAX_VALUE);
        MatchResultContainer resultContainer = new MatchResultContainer();
        resultContainer.matchResult = bestMatchResult;
        int nThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(nThreads);
        Iterator<Patient> patientIterator = patientList.iterator();
        for (int i = 0; i < nThreads; i++) {
            service.execute(new MatchCallable(a, patientIterator, resultContainer/*, times*/));
        }
        service.shutdown();
        while (!service.isTerminated()) {
        };
        return resultContainer.matchResult;
    }
}
