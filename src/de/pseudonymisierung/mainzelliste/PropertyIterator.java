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

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

public class PropertyIterator implements Iterable<Object> {

	protected final Properties props;
	protected final String prefix;
	protected final String separator;

	public PropertyIterator(Properties props, String prefix) {
		this(props, prefix, ".");
	}

	public PropertyIterator(Properties props, String prefix, String separator) {
		this.props = props;
		if (!prefix.endsWith(separator)) {
			this.prefix = prefix + separator;
		} else {
			this.prefix = prefix;
		}
		this.separator = separator;
	}

	public String getPrefix() {
		return prefix;
	}

	public synchronized Object setProperty(String key, String value) {
		return props.setProperty(resolveKey(key), value);
	}

	public String getProperty(String key) {
		return getProperty(key, null);
	}

	public String getProperty(String key, String defaultValue) {
		return props.getProperty(resolveKey(key), defaultValue);
	}

	private String resolveKey(String key) {
		if (key.equals("")) {
			return prefix.substring(0, prefix.length() - separator.length());
		} else {
			return prefix + key;
		}
	}

	public Iterable<Object> keys() {
		return new Iterable<Object>() {
			@Override
			public Iterator<Object> iterator() {
				return new PropsIterator(prefix, true);
			}
		};
	}

	@Override
	public Iterator<Object> iterator() {
		return new PropsIterator(prefix, false);
	}

	private class PropsIterator implements Iterator<Object> {

		private final Enumeration<?> keys;
		private final String localPrefix;
		private final boolean returnKeys;
		private String currentKey;

		public PropsIterator(String localPrefix, boolean returnKeys) {
			keys = props.propertyNames();
			this.localPrefix = localPrefix;
			this.returnKeys = returnKeys;
			findNext();
		}

		private void findNext() {
			while (keys.hasMoreElements()) {
				Object keyO = keys.nextElement();
				if (keyO instanceof String) {
					String key = (String) keyO;
					if (key.startsWith(localPrefix)
							&& !key.startsWith(separator, localPrefix.length() + separator.length())) {
						currentKey = key;
						return;
					}
				}
			}
			currentKey = null;
		}

		@Override
		public boolean hasNext() {
			return currentKey != null;
		}

		@Override
		public Object next() {
			String subKey = currentKey.substring(localPrefix.length());
			int sepPos = subKey.indexOf(separator);
			if (sepPos > 0) {
				return new PropertyIterator(props, localPrefix + subKey.substring(0, sepPos + separator.length()));
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
	}
}
