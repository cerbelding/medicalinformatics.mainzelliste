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

	private static final org.apache.commons.codec.language.Soundex encoder = new org.apache.commons.codec.language.Soundex();

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
		return Optional.ofNullable(encoder.encode(s));
	}

	@Override
	public String toString() {
		return "Soundex{" +
						"name='" + name + '\'' +
						", blockingFieldNames=" + blockingFieldNames +
						'}';
	}
}
