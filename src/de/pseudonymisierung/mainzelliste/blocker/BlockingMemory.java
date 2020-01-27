package de.pseudonymisierung.mainzelliste.blocker;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

/**
 * Contains the hash value and potentially additional data for blocking methods
 * The hash is used to detect changed in the blocking configuration.
 * Additional data could be determined at runtime to adapt a blocking method
 * to the actual field values.
 */
@Entity
public class BlockingMemory {

	/** Name of the blocking method */
	@Id
	@Column(nullable = false)
	private String name;

	/** Hash value of the configuration */
	@Column(nullable = false)
	private int hash;

	/** Additional data that needs to be persisted */
	@Lob
	private String data;

	public BlockingMemory(String name, int hash) {
		this.name = name;
		this.hash = hash;
	}

	protected BlockingMemory() {
	}

	public String getName() {
		return name;
	}

	public int getHash() {
		return hash;
	}

	public void setHash(int hash) {
		this.hash = hash;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
