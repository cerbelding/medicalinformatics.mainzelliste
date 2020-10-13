package de.pseudonymisierung.mainzelliste.blocker;

import de.pseudonymisierung.mainzelliste.CompoundField;
import de.pseudonymisierung.mainzelliste.Config;
import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.dto.Persistor;
import de.pseudonymisierung.mainzelliste.matcher.EpilinkMatcher;
import de.pseudonymisierung.mainzelliste.matcher.Matcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A BlockingKeyExtractor generates one or more {@link BlockingKey}s from fields of a {@link Patient}
 * that can be used to reduce the matching complexity by limiting the number of comparisons.
 */
public abstract class BlockingKeyExtractor {
	private static final String DELIMITER = ",";
	private static final String FIELDS = "fields";

	/**
	 * Name of this blocking method. Must not contain special characters, especially not '#' or '_'
	 */
	protected final String name;

	/**
	 * Names of the fields of a patient that are used by this blocking method
	 */
	protected final List<String> blockingFieldNames;

	/**
	 * Flag indicating whether the blocking keys of this method need to regenerated, e.g. due to changes
	 * in the configuration
	 */
	protected boolean needsUpdate = false;

	/**
	 * Map of blocking fieldnames to the names of exchangeable fields
	 */
	private Map<String, Set<String>> exchangeFieldNames;

	/**
	 * Allows the detection of changes in the configuration and the persistence of additional data
	 */
	protected BlockingMemory memory;

	/**
	 * Logger instance
	 */
	protected Logger logger = LogManager.getLogger(this.getClass());

	/**
	 * Construct a blockingkey extractor
	 *
	 * @param name       unqiue blocking method name
	 * @param parameters map of parameters parsed from the Mainzelliste config properties
	 */
	BlockingKeyExtractor(String name, Map<String, String> parameters) {
		assert !name.contains(BlockingKey.TYPE_VALUE_SEPARATOR) && !name.contains(BlockingKey.TYPE_SUBTYPE_SEPARATOR);
		this.name = name;
		this.blockingFieldNames = splitValue(parameters.get(BlockingKeyExtractor.FIELDS));
	}

	/**
	 * Extract a set of blocking key values for a given patient.
	 *
	 * @param patient A patient
	 * @return Set of blocking keys.
	 */
	public abstract Set<BlockingKey> extract(Patient patient);

	/**
	 * Get a hash value that is used to detect changes in the configuration of this method
	 *
	 * @return hash value
	 */
	public abstract int getConfigHash();

	/**
	 * Check whether a blocking memory exists for this method.
	 * If no, create it and request an update of the blocking keys
	 * If yes, check whether the config changed and if yes, request an update of the blocking keys
	 */
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	void checkBlockingMemory() {
		Optional<BlockingMemory> bkm = fetchBlockingMemory();
		if (bkm.isPresent()) {
			memory = bkm.get();
			if (getConfigHash() == memory.getHash()) {
				readProperties(memory.getData());
				return;
			}
			memory.setHash(getConfigHash());
		}
		needsUpdate = true;
		if (memory == null) memory = createBlockingMemory();
		Persistor.instance.updateBlockingMemory(memory);
		memory = fetchBlockingMemory().get();
	}

	/**
	 * Try to read a {@link BlockingMemory} for this method from the database
	 *
	 * @return blocking memory if it exists
	 */
	private Optional<BlockingMemory> fetchBlockingMemory() {
		final List<BlockingMemory> blockingMemories = Persistor.instance.getBlockingMemories();
		return blockingMemories.stream()
						.filter(m -> m.getName().equals(name))
						.findFirst();
	}

	/**
	 * Initialize a {@link BlockingMemory} for this method
	 *
	 * @return blocking memory (not persisted)
	 */
	private BlockingMemory createBlockingMemory() {
		return new BlockingMemory(name, getConfigHash());
	}

	/**
	 * (Re)generate the blocking keys of this method and persist them.
	 * Old blocking keys are removed.
	 *
	 * @param patients The list of patients that need an update of the blocking keys
	 */
	public void updateBlockingKeys(List<Patient> patients) {
		logger.info("Updating blocking keys for: " + this.name);
		final Collection<BlockingKey> bksNew = patients.stream()
						.flatMap(p -> extract(p).stream())
						.collect(Collectors.toList());
		logger.info("Extracted " + bksNew.size() + " new blockingkeys");

		final Collection<BlockingKey> bksOld = Persistor.instance.getBlockingKeys(patients)
						.stream()
//						.filter(bk -> patients.contains(bk.getPatient()))
						.filter(bk -> bk.getType().equals(name))
						.collect(Collectors.toList());

		logger.info("Remove " + bksOld.size() + " old blockingkeys");
		Persistor.instance.removeBlockingKeys(bksOld);
		logger.info("Add " + bksNew.size() + " new blockingkeys");
		Persistor.instance.addBlockingKeys(bksNew);
		needsUpdate = false;
	}

	boolean needsUpdate() {
		return needsUpdate;
	}

	/**
	 * Get all possible combinations of blocking fields for a patient.
	 * Example:
	 * The method uses the fields "vorname" and "nachname" and "vorname" is a {@link CompoundField}.
	 * The result of a patient with
	 * "vorname" -> ["Hans","Peter"]
	 * "nachname" -> "Meier"
	 * would be: [["Hans","Meier"],["Peter","Meier"]]
	 *
	 * @param p A patient
	 * @return combinations of blocking fields
	 */
	protected List<List<Field<?>>> getBlockingFieldCombinations(Patient p) {
		final List<Set<Field<?>>> blockingFields = getBlockingFields(p);
		final List<List<Field<?>>> blockingFieldsAsList = blockingFields.stream()
						.map(ArrayList::new)
						.collect(Collectors.toList());

		return product(blockingFieldsAsList);
	}

	/**
	 * Get the blocking fields from a patient.
	 * If a field is part of an exchange group or of type {@link CompoundField}
	 * multiple fields per blocking field are returned.
	 *
	 * @param p A patient
	 * @return list of (multiple) blocking fields
	 */
	protected List<Set<Field<?>>> getBlockingFields(Patient p) {
		final List<Set<Field<?>>> fields = new ArrayList<>();
		for (String fieldName : blockingFieldNames) {
			final Field<?> field = p.getFields().get(fieldName);
			final Set<Field<?>> curFields = new HashSet<>(getSubFields(field));
			curFields.addAll(getExchangeFields(p, fieldName));
			fields.add(curFields);
		}
		return fields;
	}

	/**
	 * Get all fields that would be used by the {@link Matcher} as part of an exchange group.
	 * If a field is of type {@link CompoundField} the primitive subtypes are returned.
	 *
	 * Example:
	 * The exchange group "nachname", "geburtsname" is used.
	 * The result for the fieldName "nachname" of a patient with
	 * "vorname" -> "Hans"
	 * "nachname" -> "Meier"
	 * "geburtsname" -> "Schmidt"
	 * would be: ["Meier","Schmidt"]
	 *
	 * @param p         A patient
	 * @param fieldName name of the blocking field
	 * @return list of primitive fields
	 */
	private Set<Field<?>> getExchangeFields(Patient p, String fieldName) {
		if (exchangeFieldNames == null) initExchangeFields();
		if (exchangeFieldNames.containsKey(fieldName)) {
			return exchangeFieldNames.get(fieldName).stream()
							.map(fn -> p.getFields().get(fn))
							.filter(Objects::nonNull)
							.map(this::getSubFields)
							.flatMap(List::stream)
							.collect(Collectors.toSet());
		} else {
			return Collections.emptySet();
		}
	}

	/**
	 * Get a list primitive fields from a potentially compound field.
	 * If the input field is a primitive than a singleton list is returned.
	 *
	 * @param field input field
	 * @return list of primitive fields
	 */
	protected List<Field<?>> getSubFields(Field<?> field) {
		if (field instanceof CompoundField<?>) {
			@SuppressWarnings("unchecked") final List<Field<?>> subFields = ((CompoundField) field).getValue();
			return subFields;
		} else {
			return Collections.singletonList(field);
		}
	}

	/**
	 * Read the exchange fields from the {@link Matcher}
	 * and create a map of fieldnames to the exchangeable fieldnames
	 */
	private void initExchangeFields() {
		exchangeFieldNames = new HashMap<>();
		final Matcher matcher = Config.instance.getMatcher();
		if (matcher instanceof EpilinkMatcher) {
			final List<List<String>> exGroups = ((EpilinkMatcher) matcher).getExchangeGroups();
			for (List<String> exGroup : exGroups) {
				for (String fieldName : exGroup) {
					if (!exchangeFieldNames.containsKey(fieldName)) {
						exchangeFieldNames.put(fieldName, new HashSet<>());
					}
					exchangeFieldNames.get(fieldName).addAll(exGroup);
				}
			}
			for (Map.Entry<String, Set<String>> eFieldNames : exchangeFieldNames.entrySet()) {
				eFieldNames.getValue().remove(eFieldNames.getKey());
			}
		}
	}

	/**
	 * Parse additional data from the blocking memory.
	 * This method should be overridden by blocking methods
	 * that use {@link BlockingKeyExtractor#writeProperties(String)}
	 * to persist data.
	 *
	 * @param data data to be persisted
	 */
	public void readProperties(String data) {
	}

	/**
	 * Write additional data to the blocking memory
	 *
	 * @param data data to be persisted
	 */
	protected void writeProperties(String data) {
		BlockingMemory mem = Persistor.instance.getBlockingMemories().stream()
						.filter(m -> m.getName().equals(name))
						.findFirst().orElse(createBlockingMemory());
		mem.setData(data);
		Persistor.instance.updateBlockingMemory(mem);
	}

	/**
	 * Util method for parsing a property value with multiple integers
	 *
	 * @param value property value
	 * @return list of integers
	 */
	protected static List<Integer> getValuesAsInt(String value) {
		return splitValue(value).stream()
						.map(Integer::parseInt)
						.collect(Collectors.toList());
	}

	/**
	 * Util method for parsing a list property value
	 *
	 * @param value property value
	 * @return A list
	 */
	protected static List<String> splitValue(String value) {
		return Arrays.stream(value.split(DELIMITER))
						.map(String::trim)
						.collect(Collectors.toList());
	}

	/**
	 * Util method for generating all combinations of list entries
	 * Example:
	 * Input: [["A","B","C"],["1","2"]]
	 * Output: [["A","1"],["A","2"],["B","1"],["B","2"],["C","1"],["C","2"]]
	 *
	 * @param inputLists list of lists
	 * @param <T>        type of the list entries
	 * @return list of possible list entry combinations
	 */
	private static <T> List<List<T>> product(List<List<T>> inputLists) {
		if (inputLists.size() >= 2) {
			List<List<T>> product = new ArrayList<>();
			for (T element : inputLists.get(0)) {
				List<T> elementList = new ArrayList<>();
				elementList.add(element);
				product.add(elementList);
			}
			for (int i = 1; i < inputLists.size(); i++) {
				product = productInner(product, inputLists.get(i));
			}
			return product;
		}
		return inputLists;
	}

	/**
	 * Util method for generating all combinations of list entries with another list.
	 * Example:
	 * Input a: [["A","1"],["B","1"],["C","1"]]
	 * Input b: ["X","Y"]
	 * Output: [["A","1","X"],["A","1","Y"],["B","1","X"],["B","1","Y"],["C","1","X"],["C","1","Y"]]
	 *
	 * @param a   list of lists
	 * @param b   list that is combined with a
	 * @param <T> type of the list entries
	 * @return list of possible list entry combinations
	 */
	private static <T> List<List<T>> productInner(List<List<T>> a, List<T> b) {
		return Optional.of(
						a.stream()
										.flatMap(e1 -> b.stream().map(e2 -> {
															List<T> curList = new ArrayList<>(e1);
															curList.add(e2);
															return curList;
														})
										)
										.collect(Collectors.toList())
		).orElse(Collections.emptyList());
	}
}
