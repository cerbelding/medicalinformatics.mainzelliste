package de.pseudonymisierung.mainzelliste.blocker;

import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.HashedField;
import de.pseudonymisierung.mainzelliste.Patient;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import de.pseudonymisierung.mainzelliste.exceptions.InternalErrorException;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * Blocking method for {@link HashedField}s using Locality Sensitive Hashing
 * with Hamming-based hash functions (HLSH).
 *
 * @see <a href="https://etd.library.vanderbilt.edu/available/etd-03262012-144837/unrestricted/dissertation.pdf">
 *       Elizabeth Durham: A framework for accurate, efficient private record linkage
 *       Diss. Vanderbilt University, 2012</a>
 *
 * @see <a href="https://www.scitepress.org/PublicationsDetail.aspx?ID=NDHmb69IuOY=&t=1">Martin Franke,
 *       Ziad Sehili, Erhard Rahm: Parallel Privacy-Preserving Record Linkage using LSH-based blocking,
 *       IoTBDS 2018, pp. 195-203 </a>
 */
public class HLsh extends BlockingKeyExtractor {
	/** Config parameter key for the number of keys */
	public static final String LSH_KEYS = "lshKeys";

	/** Config parameter key for the number of hash functions per key / key length */
	public static final String LSH_HASHES = "lshHashes";

	/** Config parameter key for the length of the input bloom filter(s) size */
	public static final String BF_SIZE = "bfSize";

	/** Config parameter key for the initializing seed of the random hash function */
	public static final String SEED = "seed";

	/** Config parameter key for the multi field strategy {@link HLshMethod} */
	public static final String LSH_METHOD = "lshMethod";

	/** Config parameter key to trigger an optimization (@link {@link #optimizeParameters(List)} on ML startup */
	public static final String INITIAL_OPTIMIZATION = "initialOptimization";

	/** Config parameter key for the share of most common bits to ignore by blocking */
	public static final String LSH_PRUNE_RATIO = "pruneRatio";

	/** Default value for the share of most common bits to ignore by blocking */
	public static final double DEFAULT_LSH_PRUNE_RATIO = 0.25;

	/** Minimum number of patients in the database to allow an optimization */
	public static final int MIN_PATIENTS_FOR_OPTIMIZATION = 500;

	/** Maximum number of patients to analyse for the optimization */
	public static final int MAX_PATIENTS_FOR_OPTIMIZATION = 5000;

	/** Property key for the persistence of the determined frequent bit positions in the {@link BlockingMemory} */
	public static final String FREQUENT_BIT_POSITION_PROPERTY = "frequentBitPositions";

	/** Number of keys per field */
	private final List<Integer> lshKeys;

	/** Number of hash functions per field */
	private final List<Integer> lshHashes;

	/** Length of the bloom filter per field */
	private final List<Integer> bfSizes;

	/** Multi field strategy */
	private final HLshMethod hLshMethod;

	/** Seed for the random hash function */
	private final long seed;

	/** Share of the most common bits to ignore */
	private double pruneRatio = DEFAULT_LSH_PRUNE_RATIO;

	/** BitSet masks for the most frequent bit positions of each field */
	private final Map<Integer, BitSet> frequentBitPositions;

	/** Field-spanning BitSet masks for the bit positions to include into the blocking keys */
	private List<BitSet> bitSetMasks;

	/** End positions of each field in the field-spanning BitSets */
	private int[] bitSetRanges;

	public HLsh(String name, Map<String, String> parameters) {
		super(name, parameters);
		this.hLshMethod = HLshMethod.from(parameters.get(LSH_METHOD));
		this.lshKeys = getValuesAsInt(parameters.get(LSH_KEYS));
		this.lshHashes = getValuesAsInt(parameters.get(LSH_HASHES));
		this.bfSizes = getValuesAsInt(parameters.get(BF_SIZE));
		this.seed = Long.parseLong(parameters.get(SEED));
		if (parameters.containsKey(LSH_PRUNE_RATIO)) {
			this.pruneRatio = Double.parseDouble(parameters.get(LSH_PRUNE_RATIO));
		}
		if (parameters.containsKey(INITIAL_OPTIMIZATION)) {
			needsUpdate = Boolean.parseBoolean(parameters.get(INITIAL_OPTIMIZATION));
		}
		this.frequentBitPositions = new HashMap<>();

		checkAndAlterConfig();
		initBitSetRanges();
		initMasks();
	}

	@Override
	public Set<BlockingKey> extract(Patient patient) {
		Set<BlockingKey> blockingKeys = new HashSet<>();

		for (List<Field<?>> blockingFieldCombination : getBlockingFieldCombinations(patient)) {
			List<BitSet> blockingBitSets = blockingFieldCombination.stream()
							.map(this::getBitSetFromField)
							.collect(Collectors.toList());
			blockingKeys.addAll(getLshKeys(blockingBitSets, patient));
		}
		for (BlockingKey blockingKey : blockingKeys) {
			blockingKey.setPatient(patient);
		}
		return blockingKeys;
	}

	@Override
	public int getConfigHash() {
		return Objects.hash(
						this.getClass().getSimpleName(),
						blockingFieldNames,
						lshKeys, lshHashes, bfSizes, hLshMethod.name(), seed, pruneRatio, frequentBitPositions);
	}

	/**
	 * Check whether the configured parameters are valid.
	 * Resolve the short notation of parameters for multiple blocking fields.
	 *
	 * Example:
	 * blocking.lsh.fields = vorname, nachname
	 * blocking.lsh.method = record
	 * blocking.lsh.lshKeys = 3
	 * blocking.lsh.lshHashes = 10
	 * ...
	 *
	 * This config is valid, although the given number of keys and hashes does not correspond to
	 * the number of fields. This is the short notation for
	 *
	 * blocking.lsh.lshKeys = 3, 3
	 * blocking.lsh.lshHashes = 10, 10
	 */
	private void checkAndAlterConfig() {
		String errorMessage = "";
		if (lshHashes.size() == 0 || lshKeys.size() == 0) {
			errorMessage = "Invalid config for blocking method " + name + ": " +
							"lshHashes and lshKeys must not be empty";
		}
		if (lshHashes.size() > 1 && lshHashes.size() != blockingFieldNames.size()) {
			errorMessage = "Invalid config for blocking method " + name + ": " +
											"If multiple lshHashes values are given, " +
											"the number must correspond to the number of blocking fields.";
		}
		if (lshKeys.size() > 1 && lshKeys.size() != blockingFieldNames.size()) {
			errorMessage = "Invalid config for blocking method " + name + ": " +
							"If multiple lshKeys values are given, " +
							"the number must correspond to the number of blocking fields.";
		}
		if (!errorMessage.isEmpty()) {
			logger.fatal(errorMessage);
			throw new InternalErrorException(errorMessage);
		}

		switch (this.hLshMethod) {
			case FIELD:
				if (lshKeys.size() == 1) lshKeys.addAll(
								Collections.nCopies(blockingFieldNames.size() - 1, lshKeys.get(0))
				);
				// No break!
			case RECORD:
				if (lshHashes.size() == 1) {
					lshHashes.addAll(Collections.nCopies(blockingFieldNames.size() - 1, lshHashes.get(0)));
				}
		}
	}

	/**
	 * Extract the blocking keys from the field bitsets.
	 * The bit positions that are used for each key are precomputed and represented as bitset masks.
	 * As the masks are based on the virtual concatenation of the field bitsets, the bit positions of
	 * it have to be mapped to the bit positions in the corresponding field bitset.
	 *
	 * @param bitSets A list of bitsets
	 * @param patient The patient the bitsets belong to.
	 * @return A set of blocking keys
	 */
	private Set<BlockingKey> getLshKeys(List<BitSet> bitSets, Patient patient) {
		final Set<BlockingKey> blockingKeys = new HashSet<>();
		for (int keyIdx = 0; keyIdx < bitSetMasks.size(); keyIdx++) {
			final BitSet bitSetMask = bitSetMasks.get(keyIdx);
			final StringBuilder bkValue = new StringBuilder();
			bitSetMask.stream()
							.forEach(i -> {
								Integer bitSetIdx = null;
								for (int bs = 0; bs < bitSetRanges.length; bs++) {
									if (i < bitSetRanges[bs]) {
										bitSetIdx = bs;
										break;
									}
								}
								if (bitSetIdx == null) {
									throw new InternalErrorException();
								}
								final int bitSetRangeStart = bitSetIdx == 0 ? 0 : bitSetRanges[bitSetIdx - 1];
								int mappedBitIndex = i - bitSetRangeStart;

								final BitSet chosenBitSet = bitSets.get(bitSetIdx);
								final boolean bitValue = chosenBitSet.get(mappedBitIndex);
								final char value = bitValue ? '1' : '0';
								bkValue.append(value);
							});
			String val = bkValue.toString();
			// Do not allow a BlockingKey that consists of zeros only
			if (!val.contains("1")) continue;
			blockingKeys.add(new BlockingKey(patient, name, String.valueOf(keyIdx), val));
		}
		return blockingKeys;
	}

	/**
	 * Initialize the internal bitset range array from the given bloom filter lengths
	 */
	private void initBitSetRanges() {
		final int numberBitVectors = this.bfSizes.size();
		bitSetRanges = new int[numberBitVectors];
		bitSetRanges[0] = this.bfSizes.get(0);
		for (int i = 1; i < numberBitVectors; i++) {
			bitSetRanges[i] = bitSetRanges[i - 1] + this.bfSizes.get(i);
		}
	}

	/**
	 * Determine the bit positions that are used for the blocking keys
	 * depending on the selected multi field strategy
	 */
	private void initMasks() {
		bitSetMasks = new ArrayList<>();
		switch (this.hLshMethod) {
			case CONCAT:
				initMasksConcat();
				break;
			case FIELD:
				initMasksField();
				break;
			case RECORD:
				initMasksRecord();
				break;
			default:
				throw new InternalErrorException();
		}
	}

	/**
	 * Determine the bit positions for the blocking keys based on the virtual concatenation of the bitsets.
	 * Bit positions of longer bitsets therefore have a higher chance of being included.
	 */
	private void initMasksConcat() {
		final int totalVirtualLength = this.bfSizes.stream().mapToInt(Integer::intValue).sum();

		for (int keyIdx = 0; keyIdx < this.lshKeys.get(0); keyIdx++) {
			final BitSet bs = new BitSet(totalVirtualLength);
			for (int hashIdx = 0; hashIdx < this.lshHashes.get(0); hashIdx++) {
				int mappedBitIndex;
				Integer bitSetIdx;
				final Random rnd = new Random(this.seed * keyIdx + hashIdx);
				int count = 0;
				int bitIdx;
				do {
					bitIdx = rnd.nextInt(totalVirtualLength);
					bitSetIdx = null;
					for (int i = 0; i < bitSetRanges.length; i++) {
						if (bitIdx < bitSetRanges[i]) {
							bitSetIdx = i;
							break;
						}
					}
					if (bitSetIdx == null) {
						throw new InternalErrorException();
					}
					final int bitSetRangeStart = bitSetIdx == 0 ? 0 : bitSetRanges[bitSetIdx - 1];
					mappedBitIndex = bitIdx - bitSetRangeStart;
					if (count > 100) break;
					count++;
				} while (this.isFrequentBitPosition(mappedBitIndex, bitSetIdx));

				bs.set(bitIdx);
			}
			bitSetMasks.add(bs);
		}
	}

	/**
	 * Determine the bit positions for the blocking keys separately for each bitset.
	 * All bit positions of a blocking key are therefore from the same field.
	 */
	private void initMasksField() {
		final int totalVirtualLength = bitSetRanges[bitSetRanges.length - 1];
		for (int fieldIdx = 0; fieldIdx < blockingFieldNames.size(); fieldIdx++) {
			for (int keyIdx = 0; keyIdx < this.lshKeys.get(fieldIdx); keyIdx++) {
				final BitSet bs = new BitSet(totalVirtualLength);
				final Random rnd = new Random(this.seed * keyIdx);
				fillBitSet(bs, rnd, fieldIdx);
				bitSetMasks.add(bs);
			}
		}
	}

	/**
	 * Determine the bit positions for the blocking keys from all bitsets.
	 * This is similar to {@link #initMasksConcat()}, but the number of hash functions per
	 * field is fixed and does not depend on the bloom filter length.
	 * The bit positions of each blocking key can be from multiple fields.
	 */
	private void initMasksRecord() {
		final int totalVirtualLength = bitSetRanges[bitSetRanges.length - 1];

		for (int keyIdx = 0; keyIdx < this.lshKeys.get(0); keyIdx++) {
			final BitSet bs = new BitSet(totalVirtualLength);
			for (int bitSetIdx = 0; bitSetIdx < blockingFieldNames.size(); bitSetIdx++) {
				final Random rnd = new Random(this.seed * keyIdx + bitSetIdx);
				fillBitSet(bs, rnd, bitSetIdx);
			}
			bitSetMasks.add(bs);
		}
	}

	/**
	 * Set the bit positions in the bitset mask for a specific field / bitset.
	 * @param bs The bitset mask to fill
	 * @param rnd The pseudorandom generator
	 * @param bitSetIdx The index of the bitset
	 */
	private void fillBitSet(BitSet bs, Random rnd, int bitSetIdx) {
		for (int hashIdx = 0; hashIdx < this.lshHashes.get(bitSetIdx); hashIdx++) {
			int bitIdx;
			do {
				bitIdx = getNextAllowedBitPosition(rnd, bitSetIdx);
				final int bitSetRangeStart = bitSetIdx == 0 ? 0 : bitSetRanges[bitSetIdx - 1];
				bitIdx = bitSetRangeStart + bitIdx;
			} while (bs.get(bitIdx));
			bs.set(bitIdx);
		}
	}

	/**
	 * Get a new random bit position for a specific field / bitset.
	 *
	 * @param rnd The pseudorandom generator
	 * @param bitSetIdx The index of the bitset
	 * @return The selected bit position
	 */
	private int getNextAllowedBitPosition(Random rnd, int bitSetIdx) {
		final int chosenBitSetSize = this.bfSizes.get(bitSetIdx);
		int bitIdx;
		int count = 0;
		do {
			bitIdx = rnd.nextInt(chosenBitSetSize);
			if (count > 100) break;
			count++;
		} while (this.isFrequentBitPosition(bitIdx, bitSetIdx));
		return bitIdx;
	}

	/**
	 * Check whether a bit position of a field is marked as frequent
	 * @param position The bit position to check
	 * @param field The field
	 */
	private boolean isFrequentBitPosition(int position, int field) {
		if (this.frequentBitPositions.containsKey(field)) {
			return this.frequentBitPositions.get(field).get(position);
		} else {
			return false;
		}
	}

	@Override
	public void readProperties(String data) {
		if (data == null || data.isEmpty()) return;
		try {
			JSONObject json = new JSONObject(data);
			for (int fn = 0; fn < this.blockingFieldNames.size(); fn++) {
				final String curFrequentBitPositionsString = json.getJSONObject(FREQUENT_BIT_POSITION_PROPERTY).getString(blockingFieldNames.get(fn));
				final BitSet curFrequentBitPositions = HashedField.base64ToBitSet(curFrequentBitPositionsString);
				this.frequentBitPositions.put(fn, curFrequentBitPositions);
			}
		} catch (JSONException e) {
			throw new InternalErrorException(e.getCause());
		}
		initMasks();
	}

	private String buildProperties() {
		final JSONObject props = new JSONObject();
		try {
			final JSONObject fbpObj = new JSONObject();
			for (Map.Entry<Integer, BitSet> entry : frequentBitPositions.entrySet()) {
				final int fi = entry.getKey();
				fbpObj.put(blockingFieldNames.get(fi), HashedField.bitSetToBase64(entry.getValue()));
			}
			props.putOpt(FREQUENT_BIT_POSITION_PROPERTY, fbpObj);
		} catch (JSONException e) {
			throw new InternalErrorException(e.getCause());
		}
		return props.toString();
	}

	@Override
	public void updateBlockingKeys(List<Patient> patients) {
		if (isOptimizationEnabled()) {
			if (patients.size() >= MIN_PATIENTS_FOR_OPTIMIZATION) {
				optimizeParameters(patients);
			} else {
				logger.info("Skipping optimization due to low number of patients.");
			}
		}
		super.updateBlockingKeys(patients);
	}

	/**
	 * Analyze the blocking fields of the patients and determine bit positions that are
	 * frequently set to '1' or '0'. These bit positions shall not be included in the blocking keys
	 * to prevent frequent blocking keys and thereby large blocks of patients.
	 *
	 * These determined bit positions are persisted in the database.
	 *
	 * @param patients A list of patients
	 */
	private void optimizeParameters(List<Patient> patients) {
		final List<Map<Integer, Long>> bitCounter = new ArrayList<>(blockingFieldNames.size());
		for (int i = 0; i < blockingFieldNames.size(); i++) bitCounter.add(new HashMap<>(bfSizes.get(i)));

		int numberOfPatientsToAnalyze = Math.min(patients.size(), MAX_PATIENTS_FOR_OPTIMIZATION);
		List<Patient> patientsForAnalysis = new ArrayList<>(patients);
		Collections.shuffle(patientsForAnalysis);
		for (final Patient patient : patientsForAnalysis.subList(0, numberOfPatientsToAnalyze)) {
			List<Set<Field<?>>> fields = getBlockingFields(patient);
			for (int fieldIdx = 0; fieldIdx < blockingFieldNames.size(); fieldIdx++) {
				for (Field<?> field : fields.get(fieldIdx)) {
					final BitSet bs = getBitSetFromField(field);
					for (int bp = 0; bp < bfSizes.get(fieldIdx); bp++) {
						if (!bitCounter.get(fieldIdx).containsKey(bp)) bitCounter.get(fieldIdx).put(bp, 0L);
						if (bs.get(bp)) bitCounter.get(fieldIdx).put(bp, bitCounter.get(fieldIdx).get(bp) + 1);
					}
				}
			}
		}

		for (int fieldIdx = 0; fieldIdx < blockingFieldNames.size(); fieldIdx++) {
			final List<Integer> sortedBitPositions = bitCounter.get(fieldIdx).entrySet().stream()
							.sorted(Map.Entry.comparingByValue())
							.map(Map.Entry::getKey)
							.collect(Collectors.toList());
			final int num = sortedBitPositions.size();
			final BitSet curFrequentBitPositions = new BitSet(num);
			if (num > 0) {
				sortedBitPositions.subList(0, (int) (num * pruneRatio))
								.forEach(curFrequentBitPositions::set);
				sortedBitPositions.subList((int) (num * (1 - pruneRatio)), num - 1)
								.forEach(curFrequentBitPositions::set);
			}
			frequentBitPositions.put(fieldIdx, curFrequentBitPositions);
			logger.info("FrequentBitPositions for " + blockingFieldNames.get(fieldIdx) + ": " +
							HashedField.bitSetToBitString(curFrequentBitPositions));
		}
		initMasks();
		writeProperties(buildProperties());
	}

	/**
	 * Extract a bitset from a field
	 * 
	 * @param field The field object
	 * @return The BitSet object
	 */
	private BitSet getBitSetFromField(Field<?> field) {
		if (field instanceof HashedField) {
			return ((HashedField) field).getValue();
		} else {
			throw new InternalErrorException("LSH only works on HashedFields!");
		}
	}

	/**
	 * The optimization can be disabled by setting the prune ratio to 0.
	 */
	private boolean isOptimizationEnabled() {
		return pruneRatio > 0;
	}

	@Override
	public String toString() {
		return "HLsh{" +
						"lshKeys=" + lshKeys +
						", lshHashes=" + lshHashes +
						", bfSizes=" + bfSizes +
						", hLshMethod=" + hLshMethod +
						", seed=" + seed +
						", pruneRatio=" + pruneRatio +
						'}';
	}
}