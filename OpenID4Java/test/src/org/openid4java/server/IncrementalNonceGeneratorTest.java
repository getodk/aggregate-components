/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.server;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
public class IncrementalNonceGeneratorTest extends AbstractNonceGeneratorTest {
  public IncrementalNonceGeneratorTest() {
    super();
  }

  public NonceGenerator createGenerator() {
    return new IncrementalNonceGenerator();
  }
}
