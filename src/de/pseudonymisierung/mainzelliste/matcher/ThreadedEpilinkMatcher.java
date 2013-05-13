/*
 * Copyright (C) 2013 Martin Lablans, Andreas Borg, Frank Ückert
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

//FIXME: Kommentar
public class ThreadedEpilinkMatcher extends EpilinkMatcher {
	private class MatchResultContainer {
		public MatchResult matchResult;
	}
	
	private class MatchCallable implements Runnable {
		
		private Iterator<Patient> iterator;
		private Patient left;
		private MatchResultContainer bestMatchResult;

		public MatchCallable(Patient left, Iterator<Patient> iterator, MatchResultContainer bestMatchResult) {
			this.iterator = iterator;
			this.left = left;
			this.bestMatchResult = bestMatchResult;
		}
	
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
	
	/* (non-Javadoc)
	 * @see de.pseudonymisierung.mainzelliste.matcher.Matcher#match(de.pseudonymisierung.mainzelliste.Patient, java.lang.Iterable)
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
