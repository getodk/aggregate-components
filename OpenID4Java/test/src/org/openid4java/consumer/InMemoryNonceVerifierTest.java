/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.consumer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
public class InMemoryNonceVerifierTest extends AbstractNonceVerifierTest {
  public NonceVerifier createVerifier(int maxAge) {
    return new InMemoryNonceVerifier(maxAge);
  }

  @Test
  public void testNonceCleanup() throws Exception {
    super.testNonceCleanup();

    InMemoryNonceVerifier inMemoryVerifier = (InMemoryNonceVerifier) _nonceVerifier;

    assertEquals(1, inMemoryVerifier.size());
  }
}
