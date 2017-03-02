/*
 * Copyright (C) 2013-2015 Martin Lablans, Andreas Borg, Frank Ückert
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
package de.pseudonymisierung.mainzelliste;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;

/**
 * This class maps the flat property system to a tree-like structure.
 * Java properties are a flat key->value map without any possibility to
 * access the properties in a hierarchical way.
 * This class overcomes that lag by defining a prefix that is added
 * automatically to every read and write action. It also allows to iterate over a
 * subset of the properties which contains only those keys starting with the prefix.
 *
 * Assume the following properties map:
 *  bar.foo = 123
 *  foo.bar1 = foo
 *  foo.bar2 = bar
 *  foo.bar3 = test
 *  test = test2
 *
 * To operating on all keys starting with foo, cretae an instance of PropertyIterator:
 *   PropertyIterator pi = new PropertyIterator(props, "foo");
 * This constructor sets the path separator to ".". The path separator is added
 * automatically to the prefix as needed. props is an instance of java.util.Properties
 * To access the property foo.bar1 just call
 *   pi.getProperty("bar1").
 * To iterate over all properties starting with "foo." use a for-each loop:
 *   for (String value : pi) {}
 *
 * @author Daniel Volk <volk@izks-mainz.de>
 */
public class PropertyIterator implements Serializable {
	private static final long serialVersionUID = 1L;

	protected final Properties props;
	protected final String prefix;
	protected final String separator;

	/**
	 * Creates a PropertyIterator with the default path separator.
	 *
	 * @param props The properties to operate on
	 * @param prefix The prefix to filter the properties
	 */
	public PropertyIterator(Properties props, String prefix) {
		this(props, prefix, ".");
	}

	/**
	 * Creates a PropertyIterator with the default path separator.
	 *
	 * @param props The properties to operate on
	 * @param prefix The prefix to filter the properties
	 * @param separator The path separator to use
	 */
	public PropertyIterator(Properties props, String prefix, String separator) {
		if (props == null) {
			throw new IllegalArgumentException("Parent properties cannot be null");
		}
		if (prefix == null) {
			throw new IllegalArgumentException("prefix cannot be null");
		}
		this.props = props;
		if ((prefix.length() > 0) && !prefix.endsWith(separator)) {
			this.prefix = prefix + separator;
		} else {
			this.prefix = prefix;
		}
		this.separator = separator;
	}

	/**
	 * Return the prefix
	 *
	 * @return The prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Check if the properties contains the specified key
	 *
	 * @param key The key to search for
	 * @return True when the properties contains the key
	 */
	public boolean containsKey(String key) {
		return props.containsKey(resolveKey(key));
	}

	/**
	 * Set the value of the specified property.
	 *
	 * @param key The property to set
	 * @param value The new value
	 * @return the previous value of the specified property, or {@code null} if it did not have one.
	 */
	public synchronized Object setProperty(String key, String value) {
		return props.setProperty(resolveKey(key), value);
	}

	/**
	 * Get the value of the specified property.
	 *
	 * @param key The property to get
	 * @return The value of the property
	 */
	public String getProperty(String key) {
		return getProperty(key, null);
	}

	/**
	 * Get the value of the specified property. If the property is not found the default value will be returned
	 *
	 * @param key The property to get
	 * @param defaultValue The default value
	 * @return The value of the property
	 */
	public String getProperty(String key, String defaultValue) {
		return props.getProperty(resolveKey(key), defaultValue);
	}

	/**
	 * Returns a new PropertyIterator with the specified key as prefix.
	 *
	 * @param key {@code this.prefix} + {@code key} is the prefix for the new {@code PropertyIterator}
	 * @return The new PropertyIterator
	 */
	public PropertyIterator getProperties(String key) {
		return getProperties(key, separator);
	}


	/**
	 * Returns a new PropertyIterator with the specified key as prefix.
	 *
	 * @param key {@code this.prefix} + {@code key} is the prefix for the new {@code PropertyIterator}
	 * @param separator The new path separator for the new key subset
	 * @return The new PropertyIterator
	 */
	public PropertyIterator getProperties(String key, String separator) {
		if (!containsKey(key)) {
			return null;
		}
		return new PropertyIterator(props, resolveKey(key), separator);
	}

	/** Resolve the key by adding the prefix. */
	private String resolveKey(String key) {
		if (key.equals("")) {
			return prefix.substring(0, prefix.length() - separator.length());
		} else {
			return prefix + key;
		}
	}

	/**
	 * Returns an iterator to iterate over the keys of this PropertyIterator.
	 * This creates a key iterator with the separator of this PropertyIterator
	 * @see keyIterator(String separator)
	 * @return An iterator over the sub keys of the prefix
	 */
	public Iterable<Object> keyIterator() {
		return new Iterable<Object>() {
			@Override
			public Iterator<Object> iterator() {
				return new PropsIterator(prefix, separator, true);
			}
		};
	}

	/**
	 * Returns an iterator to iterate over the keys of this PropertyIterator.
	 * Each returned object from the next() method is whether a string representing a key
	 * or an instance of a new PropertyIterator if the found key is a root of other sub keys.
	 * Assume the following structure:
	 *   bar.foo = 123
	 *   foo.bar1 = foo
	 *   foo.bar2.b1 = bar2
	 *   foo.bar2.b2 = bar
	 *   foo.bar3 = test
	 *   test = test2
	 * Iterating over a PropertyIterator with the prefix "foo" will return:
	 *   "bar1"
	 *   A PropertyIterator with the prefix "foo.bar2"
	 *   "bar3"
	 * @param separator The path separator for the new key subset
	 * @return An iterator over the sub keys of the prefix
	 */
	public Iterable<Object> keyIterator(final String separator) {
		return new Iterable<Object>() {
			@Override
			public Iterator<Object> iterator() {
				return new PropsIterator(prefix, separator, true);
			}
		};
	}

	/**
	 * The internal readonly property iterator.
	 * This class does the magic when iterating over the sub key:
	 * - searching the next matching key in the underlaying properties
	 * - create new PropertyIterator if the found key is a root key itself
	 */
	private class PropsIterator implements Iterator<Object> {

		private final Enumeration<?> keys;
		private final String localPrefix;
		private final String newSeparator;
		private final boolean returnKeys;
		private String currentKey;

		public PropsIterator(String localPrefix, String newSeparator, boolean returnKeys) {
			keys = props.propertyNames();
			this.localPrefix = localPrefix;
			this.newSeparator = newSeparator;
			this.returnKeys = returnKeys;
			findNext();
		}

		/** internal method for finding the next matching key. */
		private void findNext() {
			while (keys.hasMoreElements()) {
				Object keyO = keys.nextElement();
				if (keyO instanceof String) {
					String key = (String) keyO;
					if (key.startsWith(localPrefix)
							&& !key.startsWith(newSeparator, localPrefix.length() + newSeparator.length())) {
						currentKey = key;
						return;
					}
				}
			}
			currentKey = null;
		}

		/**
		 * Returns {@code true} if the iteration has more elements.
		 *
		 * @return {@code true} if the iteration has more elements
		 */
		@Override
		public boolean hasNext() {
			return currentKey != null;
		}

		/**
		 * Returns the next element in the iteration.
		 *
		 * @return the next element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		public Object next() {
			if (currentKey == null)
				throw new NoSuchElementException();

			String subKey = currentKey.substring(localPrefix.length());
			int sepPos = subKey.indexOf(newSeparator);
			if (sepPos > 0) {
				return new PropertyIterator(props, localPrefix + subKey.substring(0, sepPos + newSeparator.length()));
			}

			Object output;
			if (returnKeys) {
				output = subKey;
			} else {
				output = props.getProperty(currentKey);
			}

			findNext();

			return output;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Not supported yet."); 
		}
	}
}
