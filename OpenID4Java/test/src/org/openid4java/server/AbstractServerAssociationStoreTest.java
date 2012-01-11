/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.server;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.openid4java.association.Association;
import org.openid4java.association.AssociationException;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
public abstract class AbstractServerAssociationStoreTest {
  protected ServerAssociationStore _associationStore;

  public AbstractServerAssociationStoreTest() {
  }

  @Before
  public void setUp() throws Exception {
    _associationStore = createStore();
  }

  public abstract ServerAssociationStore createStore();

  @Test
  public void testGenerate() throws AssociationException {
    Association association = _associationStore.generate(Association.TYPE_HMAC_SHA1, 60);

    assertNotNull(association);

    assertSame(association, _associationStore.load(association.getHandle()));

    association = _associationStore.generate(Association.TYPE_HMAC_SHA256, 60);

    assertNotNull(association);

    assertSame(association, _associationStore.load(association.getHandle()));
  }

  @Test
  public void testGenerateBadType() {
    try {
      String badType = "xyz";
      _associationStore.generate(badType, 60);

      fail("Should throw exception for bad associtation type: " + badType);
    } catch (AssociationException e) {
    }
  }

  @Test
  public void testLoad() throws AssociationException {
    assertNull(_associationStore.load(null));
    assertNull(_associationStore.load(""));
    assertNull(_associationStore.load("xyz"));

    String handle = _associationStore.generate(Association.TYPE_HMAC_SHA1, 60).getHandle();

    assertNotNull(_associationStore.load(handle));
    assertNotNull(_associationStore.load(handle));
  }

  @Test
  public void testExpiry() throws AssociationException, InterruptedException {
    String handle = _associationStore.generate(Association.TYPE_HMAC_SHA1, 1).getHandle();

    assertNotNull(_associationStore.load(handle));
    Thread.sleep(2000);
    assertNull(_associationStore.load(handle));
  }

  @Test
  public void testRemove() throws AssociationException {
    String handle = _associationStore.generate(Association.TYPE_HMAC_SHA1, 1).getHandle();

    assertNotNull(_associationStore.load(handle));
    _associationStore.remove(handle);
    assertNull(_associationStore.load(handle));
  }

  // overridden by derived classes
  public void testCleanup() throws AssociationException, InterruptedException {
    _associationStore.generate(Association.TYPE_HMAC_SHA1, 1);
    _associationStore.generate(Association.TYPE_HMAC_SHA1, 1);
    _associationStore.generate(Association.TYPE_HMAC_SHA1, 1);
    _associationStore.generate(Association.TYPE_HMAC_SHA1, 1);

    Thread.sleep(2000);

    _associationStore.generate(Association.TYPE_HMAC_SHA1, 1);
  }
}
