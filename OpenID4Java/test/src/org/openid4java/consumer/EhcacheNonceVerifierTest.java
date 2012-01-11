/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.consumer;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
public class EhcacheNonceVerifierTest extends AbstractNonceVerifierTest {
  private CacheManager _cacheManager;

  @Before
  public void setUp() throws Exception {
    _cacheManager = new CacheManager();

    super.setUp();
  }

  @After
  public void tearDown() throws Exception {
    super.tearDown();

    _cacheManager = null;
  }

  public NonceVerifier createVerifier(int maxAge) {
    _cacheManager.removalAll();
    _cacheManager.addCache(new Cache("testCache", 100, false, false, maxAge, maxAge));

    EhcacheNonceVerifier nonceVerifier = new EhcacheNonceVerifier(maxAge);
    nonceVerifier.setCache(_cacheManager.getCache("testCache"));

    return nonceVerifier;
  }

  @Test
  public void testNonceCleanup() throws Exception {
    super.testNonceCleanup();
  }
}
