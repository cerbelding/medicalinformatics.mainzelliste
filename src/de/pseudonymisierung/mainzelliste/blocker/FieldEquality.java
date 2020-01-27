package de.pseudonymisierung.mainzelliste.blocker;

import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.Patient;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Simple blocking method based on the equality of fields.
 * Patients are grouped together if all of the configured fields are equal.
 *
 * A potential usage scenario would be the date of birth, represented as
 * three separate fields for day, month and year.
 * A configuration where three blocking keys are defined each with two of the three
 * fields would result in blocks of patients that share
 * - the same day and month of birth
 * - OR the same day and year of birth
 * - OR the same month and year of birth
 */
public class FieldEquality extends BlockingKeyExtractor {

	public FieldEquality(String name, Map<String, String> parameters) {
		super(name, parameters);
	}

	@Override
	public Set<BlockingKey> extract(Patient patient) {
		final Set<BlockingKey> blockingKeys = new HashSet<>();

		for (List<Field<?>> fields : getBlockingFieldCombinations(patient)) {
			StringBuilder sb = new StringBuilder();
			for (Field<?> field : fields) {
				sb.append(field.toString());
			}
			String val = sb.toString();
			if (val.isEmpty()) continue;
			BlockingKey bk = new BlockingKey(patient, name, val);
			blockingKeys.add(bk);
		}
		return blockingKeys;
	}

	@Override
	public int getConfigHash() {
		return Objects.hash(
						this.getClass().getSimpleName(),
						blockingFieldNames
		);
	}

	@Override
	public String toString() {
		return "FieldEquality{" +
						"name='" + name + '\'' +
						", blockingFieldNames=" + blockingFieldNames +
						'}';
	}
}
