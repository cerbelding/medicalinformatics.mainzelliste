package de.pseudonymisierung.mainzelliste.blocker;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Wrapper for all used {@link BlockingKeyExtractor}s.
 */
public class BlockingKeyExtractors {

	/** The blocking methods */
	private List<BlockingKeyExtractor> blockingKeyExtractors;

	/** The logging instance */
	static Logger logger = LogManager.getLogger(BlockingKeyExtractors.class);

	/**
	 * Initialize from the configuration properties
	 * @param props properties of the Mainzelliste
	 * @throws InternalErrorException If an error occurs during initialization.
	 */
	public BlockingKeyExtractors(Properties props) throws InternalErrorException {
		this.blockingKeyExtractors = new ArrayList<>();

		// Get names of blockingkeys from config
		Pattern p = Pattern.compile("^blocking\\.(\\w+)\\.type");
		java.util.regex.Matcher m;

		// Build list of blockingKeyExtractor instances from Properties
		for (Object key : props.keySet()) {
			m = p.matcher((String) key);
			if (m.find()) {
				final String bkName = m.group(1);
				String typeProp = props.getProperty("blocking." + bkName + ".type");
				typeProp = typeProp.trim();

				try {
					Constructor<?> c = Class.forName("de.pseudonymisierung.mainzelliste.blocker." + typeProp)
									.getConstructor(String.class, Map.class);
					BlockingKeyExtractor bke = (BlockingKeyExtractor) c.newInstance(bkName, getParamMap(bkName, props));
					blockingKeyExtractors.add(bke);
					logger.info("Initialized blocking key extractor: " + bke);
				} catch (Exception e) {
					throw new InternalErrorException(e.getMessage());
				}
			}
		}
	}

	/**
	 * Extract a set of blocking key values for a given patient.
	 * @param p A patient
	 * @return The blocking keys
	 */
	public Set<BlockingKey> extract(Patient p) {
		Set<BlockingKey> bks = new HashSet<>();
		for (BlockingKeyExtractor blockingMethod : blockingKeyExtractors) {
			bks.addAll(blockingMethod.extract(p));
		}
		return bks;
	}

	/**
	 * (Re)generate blocking keys if a blocking method was changed or added.
	 * Blocking keys of previously used methods are removed.
	 */
	public void updateBlockingKeyExtractors() {
		long t1 = System.currentTimeMillis();
		List<Patient> patients = null;

		List<BlockingMemory> blockingMemories = Persistor.instance.getBlockingMemories();
		Set<String> previousBlockingMethods = blockingMemories.stream().map(BlockingMemory::getName).collect(Collectors.toSet());

		for (BlockingKeyExtractor bke : blockingKeyExtractors) {
			bke.checkBlockingMemory();
			if (bke.needsUpdate()) {
				if (patients == null) patients = Persistor.instance.getPatients();
				bke.updateBlockingKeys(patients);
			}
			previousBlockingMethods.remove(bke.name);
		}

		for (String previousBlockingMethod : previousBlockingMethods) {
			List<BlockingKey> orphanedBlockingKeys = Persistor.instance.getBlockingKeys().stream()
							.filter(bk -> bk.getType().equals(previousBlockingMethod))
							.collect(Collectors.toList());
			logger.info("Blocking config " + previousBlockingMethod + " was removed. Deleting " +
							orphanedBlockingKeys.size() + " orphaned blocking keys.");
			Persistor.instance.removeBlockingKeys(orphanedBlockingKeys);
		}
		Persistor.instance.removeBlockingMemories(
						blockingMemories.stream()
										.filter(mem -> previousBlockingMethods.contains(mem.getName()))
										.collect(Collectors.toList())
		);

		long t2 = System.currentTimeMillis();
		logger.info("Blocking keys update time in ms: " + (t2 - t1));
	}

	/**
	 * (Re)generate the blocking keys for the given patients and persist them.
	 * Old blocking keys are removed.
	 *
	 * @param patients The list of patients that need an update of the blocking keys
	 */
	public void updateBlockingKeys(List<Patient> patients) {
		for (BlockingKeyExtractor bke : blockingKeyExtractors) {
			bke.updateBlockingKeys(patients);
		}
	}

	/**
	 * Extract the configuration parameters of a blocking method from the properties
	 * @param bkName name of the blocking method
	 * @param props Mainzelliste properties
	 * @return map of parameters
	 */
	private Map<String, String> getParamMap(String bkName, Properties props) {
		Pattern p = Pattern.compile("^blocking\\." + bkName + "\\.(\\w+)");
		java.util.regex.Matcher m;

		Map<String, String> params = new HashMap<>();
		for (Object key : props.keySet()) {
			m = p.matcher((String) key);
			if (m.find()) {
				final String bkParamName = m.group(1);
				final String bkParamVal = props.getProperty("blocking." + bkName + "." + bkParamName);
				params.put(bkParamName, bkParamVal);
			}
		}
		return params;
	}
}