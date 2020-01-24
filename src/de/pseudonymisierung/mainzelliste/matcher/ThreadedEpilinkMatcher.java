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

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.matcher.MatchResult.MatchResultType;

/**
 * Multithreaded version of {@link EpilinkMatcher EpilinkMatcher}. Distributes
 * matching to several threads, each processing a subset of the patients to
 * match with.
 */
public class ThreadedEpilinkMatcher extends EpilinkMatcher {

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
		/** The (new) patient which to match to the patient list. */
		private Patient left;
		/** The best match this worker finds. */
		private MatchResultContainer bestMatchResult;


		/**
		 * Create an instance.
		 *
		 * @param left
		 *            The (new) patient which to compare against the others.
		 * @param iterator
		 *            Queue from which to draw patients to match against.
		 * @param bestMatchResult
		 *            Object to store the best match result in.
		 */
		public MatchCallable(Patient left, Iterator<Patient> iterator, MatchResultContainer bestMatchResult) {
			this.iterator = iterator;
			this.left = left;
			this.bestMatchResult = bestMatchResult;
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
					if (!iterator.hasNext()) break;
					right = iterator.next();
				}
				double thisWeight = calculateWeight(left, right);
				synchronized(bestMatchResult) {
					if (thisWeight > bestMatchResult.matchResult.getBestMatchedWeight()) {
						MatchResultType t;
						if (thisWeight >= getThresholdMatch())
							t = MatchResultType.MATCH;
						else if (thisWeight >= getThresholdNonMatch())
							t = MatchResultType.POSSIBLE_MATCH;
						else
							t = MatchResultType.NON_MATCH;
						bestMatchResult.matchResult = new MatchResult(t, right, thisWeight);
					}
				}
			} while (true);
		}
	}

	/**
	 * Puts all patients to match against in a queue and creates a set of workers to process the queue.
	 * @see MatchCallable
	 */
	@Override
	public MatchResult match(Patient a, Iterable<Patient> patientList) {

		MatchResult bestMatchResult = new MatchResult(MatchResultType.NON_MATCH, null, -Double.MAX_VALUE);
		MatchResultContainer resultContainer = new MatchResultContainer();
		resultContainer.matchResult = bestMatchResult;
		int nThreads = Runtime.getRuntime().availableProcessors();
		ExecutorService service = Executors.newFixedThreadPool(nThreads);
		Iterator<Patient> patientIterator = patientList.iterator();
		for (int i=0; i < nThreads; i++) {
			service.execute(new MatchCallable(a, patientIterator, resultContainer));
		}
		service.shutdown();
		while (!service.isTerminated()) {};
		return resultContainer.matchResult;
	}


}
