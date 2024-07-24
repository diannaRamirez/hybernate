package org.hibernate.orm.test.id.uuid.custom;

import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.UUIDGenerationStrategy;
import org.hibernate.id.uuid.StandardRandomStrategy;
import org.hibernate.id.uuid.UuidValueGenerator;

public class UuidV7ValueGenerator implements UUIDGenerationStrategy, UuidValueGenerator {
	public static final UuidV7ValueGenerator INSTANCE = new UuidV7ValueGenerator();

	/**
	 * A variant 7
	 */
	@Override
	public int getGeneratedVersion() {
		// UUID v7
		return 7;
	}

	/**
	 * Delegates to {@link UUID#randomUUID()}
	 */
	@Override
	public UUID generateUUID(SharedSessionContractImplementor session) {
		return generateUuid( session );
	}

	@Override
	public UUID generateUuid(SharedSessionContractImplementor session) {
		return UUIDv7.nextIdentifier();
	}
}
