package org.doomdark.uuid;

/**
 * Remove this archaic dependency. Don't worry about efficiency.
 *
 * @author mitchellsundt@gmail.com
 *
 */
public class UUIDGenerator {

	private UUIDGenerator() {

	}

	private static UUIDGenerator instance = new UUIDGenerator();

	public UUIDGenerator getInstance() {
		return instance;
	}

	public UUID generateTimeBasedUUID() {
		return new UUID(java.util.UUID.randomUUID().toString().toLowerCase());
	}
}
