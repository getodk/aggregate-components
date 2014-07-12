package org.doomdark.uuid;

/**
 * Remove this archaic dependency. Don't worry about efficiency.
 *
 * @author mitchellsundt@gmail.com
 *
 */
public class UUID {
	String id;

	UUID(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return id;
	}
}
