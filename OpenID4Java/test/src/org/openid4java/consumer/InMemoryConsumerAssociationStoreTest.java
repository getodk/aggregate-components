/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.consumer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Marius Scurtescu
 */
public class InMemoryConsumerAssociationStoreTest extends ConsumerAssociationStoreTest {
  protected ConsumerAssociationStore createStore() {
    return new InMemoryConsumerAssociationStore();
  }

  @Test
  public void testCleanup() throws InterruptedException {
    super.testCleanup();

    InMemoryConsumerAssociationStore inMemoryAssociationStore = (InMemoryConsumerAssociationStore) _associationStore;
    assertEquals(1, inMemoryAssociationStore.size());
  }
}
