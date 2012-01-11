/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openid4java.association.AssociationException;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
public class InMemoryServerAssociationStoreTest extends AbstractServerAssociationStoreTest {
  public InMemoryServerAssociationStoreTest() {
    super();
  }

  public ServerAssociationStore createStore() {
    return new InMemoryServerAssociationStore();
  }

  @Test
  public void testCleanup() throws AssociationException, InterruptedException {
    super.testCleanup();

    InMemoryServerAssociationStore inMemoryAssociationStore = (InMemoryServerAssociationStore) _associationStore;

    assertEquals(1, inMemoryAssociationStore.size());
  }
}
