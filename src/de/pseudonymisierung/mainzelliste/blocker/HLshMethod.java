package de.pseudonymisierung.mainzelliste.blocker;

/**
 * Definition of the strategy for {@link HLsh} blocking on multiple fields
 */
public enum HLshMethod {

	RECORD("record"),
	FIELD("field");

	private String name;

	HLshMethod(String name) {
		this.name = name;
	}

	public static HLshMethod from(String s) {
		for (final HLshMethod hLshMethod : HLshMethod.values()) {
			if (hLshMethod.name.equalsIgnoreCase(s)) {
				return hLshMethod;
			}
		}
		throw new IllegalArgumentException("No constant " + s + " found");
	}
}
