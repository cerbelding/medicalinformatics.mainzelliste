package de.pseudonymisierung.mainzelliste.blocker;

import de.pseudonymisierung.mainzelliste.Field;
import de.pseudonymisierung.mainzelliste.Patient;
import de.pseudonymisierung.mainzelliste.PlainTextField;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Blocking method for {@link PlainTextField}s based on the Soundex phonetic encoding.
 * Words with a similar pronunciation (in the english language) get the same
 * blocking key and are therefore grouped together.
 *
 *  @see <a href="https://www.archives.gov/research/census/soundex.html">Soundex coding rules</a>
 */
public class Soundex extends BlockingKeyExtractor {

	public Soundex(String name, Map<String, String> parameters) {
		super(name, parameters);
	}

	@Override
	public Set<BlockingKey> extract(Patient patient) {
		final Set<BlockingKey> blockingKeys = new HashSet<>();

		List<Set<Field<?>>> blockingFields = getBlockingFields(patient);
		for (int i = 0; i < blockingFields.size(); i++) {
			for (Field<?> subField : blockingFields.get(i)) {
				if (subField instanceof PlainTextField) {
					String fieldVal = ((PlainTextField) subField).getValue();
					Optional<String> blkVal = computeSoundex(fieldVal);
					if (blkVal.isPresent()) {
						BlockingKey bk = new BlockingKey(
										patient,
										name,
										blockingFields.size() > 1 ? String.valueOf(i) : null,
										blkVal.get()
						);
						blockingKeys.add(bk);
					}
				}
			}
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

	private static Optional<String> computeSoundex(String s) {
		if (s == null || s.length() <= 0) return Optional.empty();
		char[] x = s.toUpperCase().toCharArray();
		char firstLetter = x[0];
		for (int i = 0; i < x.length; i++) {
			switch (x[i]) {
				case 'B':
				case 'F':
				case 'P':
				case 'V':
					x[i] = '1';
					break;

				case 'C':
				case 'G':
				case 'J':
				case 'K':
				case 'Q':
				case 'S':
				case 'X':
				case 'Z':
					x[i] = '2';
					break;

				case 'D':
				case 'T':
					x[i] = '3';
					break;

				case 'L':
					x[i] = '4';
					break;

				case 'M':
				case 'N':
					x[i] = '5';
					break;

				case 'R':
					x[i] = '6';
					break;

				default:
					x[i] = '0';
					break;
			}
		}

		// remove duplicates
		String output = "" + firstLetter;
		for (int i = 1; i < x.length; i++)
			if (x[i] != x[i - 1] && x[i] != '0')
				output += x[i];

		// pad with 0's or truncate
		output = output + "0000";
		return Optional.of(output.substring(0, 4));
	}

	@Override
	public String toString() {
		return "Soundex{" +
						"name='" + name + '\'' +
						", blockingFieldNames=" + blockingFieldNames +
						'}';
	}
}
