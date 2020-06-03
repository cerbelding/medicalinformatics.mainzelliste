package de.pseudonymisierung.mainzelliste.blocker;

import org.apache.openjpa.persistence.jdbc.Index;
import org.codehaus.jackson.annotate.JsonIgnore;

import de.pseudonymisierung.mainzelliste.Patient;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * A blocking key is a feature of a patient that can be used to group patients. Only patients
 * that share the same key and are therefore in the same group are compared to each other to reduce
 * the matching complexity.
 * However this can lead to false negatives as patients that are similar with respect to their
 * fields but do not share the same blocking key are never compared.
 * Therefore multiple blocking keys of different type should be used.
 *
 * A blocking key is composed of a type (the name of the method) and a value.
 *
 * The value is only used for equality checks and could therefore be hashed e.g. using SHA.
 * This would prevent potentially very long values. However, this would also prevent a useful
 * analysis of the blocking strategy as the values of large blocks could not be interpreted.
 */
@Entity
public class BlockingKey {
	static final String TYPE_VALUE_SEPARATOR = "#";
	static final String TYPE_SUBTYPE_SEPARATOR = "_";

	/**
	 * Construct a blocking key.
	 * @param patient The patient linked to this key
	 * @param type The name of the blocking method, e.g. "soundex"
	 * @param value The value e.g. "S622"
	 */
	public BlockingKey(Patient patient, String type, String value) {
		this.key = buildKey(type, value);
		this.patient = patient;
	}

	/**
	 * Construct a blocking key with a subtype. Some blocking methods create multiple
	 * keys that are of the same type but not exchangeable.
	 *
	 * @param patient The patient linked to this key
	 * @param type The name of the blocking method, e.g. "lsh"
	 * @param subType The name of the subtype e.g. "0"
	 * @param value The value e.g. "1001011"
	 */
	public BlockingKey(Patient patient, String type, String subType, String value) {
		this(patient, buildType(type, subType), value);
	}

	protected BlockingKey() {
	}

	/** Database id */
	@Id
	@GeneratedValue
	@JsonIgnore
	private int bkJpaId;

	/** The patient */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "patientJpaId", nullable = false)
	private Patient patient;

	/** The key composed of the type and the value */
	@Column(name = "bk", nullable = false)
	@Index
	private String key;

	/**
	 * Create the composed type from the core type and the subtype
	 * @param type The name of the blocking method
	 * @param subType The name of the subType
	 * @return A composed type string
	 */
	private static String buildType(String type, String subType) {
		return type + ((subType != null && !subType.isEmpty()) ? TYPE_SUBTYPE_SEPARATOR + subType : "");
	}

	/**
	 * Create the key composed of the type and the value
	 * @param type The (composed) name of the blocking method
	 * @param value The value of the eky
	 * @return A composed key string
	 */
	private static String buildKey(String type, String value) {
		return type + TYPE_VALUE_SEPARATOR + value;
	}

	public Patient getPatient() {
		return patient;
	}

	public void setPatient(Patient patient) {
		this.patient = patient;
	}

	public String getKey() {
		return key;
	}

	public String getType() {
		String[] parts = key.split(TYPE_VALUE_SEPARATOR, 2);
		return parts[0].split(TYPE_SUBTYPE_SEPARATOR, 2)[0];
	}

	public String getValue() {
		return key.split(TYPE_VALUE_SEPARATOR, 2)[1];
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof BlockingKey)) {
			return false;
		}
		BlockingKey other = (BlockingKey) obj;
		if (!key.equals(other.key)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "BlockingKey{" +
						"patientJpaId=" + (patient == null ? null : patient.getPatientJpaId()) +
						", type='" + getType() + '\'' +
						", value='" + getValue() + '\'' +
						'}';
	}
}