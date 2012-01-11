/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.consumer;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openid4java.server.IncrementalNonceGenerator;
import org.openid4java.server.NonceGenerator;
import org.openid4java.util.InternetDateFormat;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
public abstract class AbstractNonceVerifierTest {
  protected NonceVerifier _nonceVerifier;
  protected InternetDateFormat _dateFormat = new InternetDateFormat();
  public static final int MAX_AGE = 60;

  @Before
  public void setUp() throws Exception {
    _nonceVerifier = createVerifier(MAX_AGE);
  }

  public abstract NonceVerifier createVerifier(int maxAge);

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testSeen() {
    String nonce = _dateFormat.format(new Date()) + "abc";

    assertEquals(NonceVerifier.OK, _nonceVerifier.seen("op1", nonce));
    assertEquals(NonceVerifier.SEEN, _nonceVerifier.seen("op1", nonce));

    assertEquals(NonceVerifier.OK, _nonceVerifier.seen("op2", nonce));
  }

  @Test
  public void testMalformed() {
    assertEquals(NonceVerifier.INVALID_TIMESTAMP, _nonceVerifier.seen("op1", "xyz"));
  }

  @Test
  public void testExpired() {
    Date now = new Date();
    Date past = new Date(now.getTime() - (MAX_AGE + 1) * 1000);

    String nonce = _dateFormat.format(past) + "abc";

    assertEquals(NonceVerifier.TOO_OLD, _nonceVerifier.seen("op1", nonce));
  }

  // overridden by derived class
  public void testNonceCleanup() throws Exception {
    NonceGenerator nonceGenerator = new IncrementalNonceGenerator();
    _nonceVerifier = createVerifier(1);

    assertEquals(NonceVerifier.OK, _nonceVerifier.seen("http://example.com", nonceGenerator.next()));
    assertEquals(NonceVerifier.OK, _nonceVerifier.seen("http://example.com", nonceGenerator.next()));
    assertEquals(NonceVerifier.OK, _nonceVerifier.seen("http://example.com", nonceGenerator.next()));
    assertEquals(NonceVerifier.OK, _nonceVerifier.seen("http://example.com", nonceGenerator.next()));

    assertEquals(NonceVerifier.OK, _nonceVerifier.seen("http://example.net", nonceGenerator.next()));
    assertEquals(NonceVerifier.OK, _nonceVerifier.seen("http://example.net", nonceGenerator.next()));
    assertEquals(NonceVerifier.OK, _nonceVerifier.seen("http://example.net", nonceGenerator.next()));
    assertEquals(NonceVerifier.OK, _nonceVerifier.seen("http://example.net", nonceGenerator.next()));

    Thread.sleep(1000);

    assertEquals(NonceVerifier.OK, _nonceVerifier.seen("http://example.org", nonceGenerator.next()));
  }
}
