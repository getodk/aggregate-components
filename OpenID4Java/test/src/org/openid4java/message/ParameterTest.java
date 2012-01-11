/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
public class ParameterTest {
  @Test
  public void testEquals() throws Exception {
    Parameter parameter1 = new Parameter("key", "value");
    Parameter parameter2 = new Parameter("key", "value");

    assertEquals(parameter1, parameter2);
    assertNotSame(parameter1, parameter2);

    parameter1 = new Parameter("", "value");
    parameter2 = new Parameter("", "value");

    assertEquals(parameter1, parameter2);
    assertNotSame(parameter1, parameter2);

    parameter1 = new Parameter("", "");
    parameter2 = new Parameter("", "");

    assertEquals(parameter1, parameter2);
    assertNotSame(parameter1, parameter2);

    parameter1 = new Parameter(null, "");
    parameter2 = new Parameter(null, "");

    assertEquals(parameter1, parameter2);
    assertNotSame(parameter1, parameter2);

    parameter1 = new Parameter(null, null);
    parameter2 = new Parameter(null, null);

    assertEquals(parameter1, parameter2);
    assertNotSame(parameter1, parameter2);

    parameter1 = new Parameter("key", "value1");
    parameter2 = new Parameter("key", "value2");

    assertFalse(parameter1.equals(parameter2));
    assertNotSame(parameter1, parameter2);
  }

  @Test
  public void testHashCode() throws Exception {
    Parameter parameter1 = new Parameter("key", "value");
    Parameter parameter2 = new Parameter("key", "value");

    assertEquals(parameter1.hashCode(), parameter2.hashCode());
    assertNotSame(parameter1, parameter2);

    parameter1 = new Parameter("", "value");
    parameter2 = new Parameter("", "value");

    assertEquals(parameter1.hashCode(), parameter2.hashCode());
    assertNotSame(parameter1, parameter2);

    parameter1 = new Parameter("", "");
    parameter2 = new Parameter("", "");

    assertEquals(parameter1.hashCode(), parameter2.hashCode());
    assertNotSame(parameter1, parameter2);

    parameter1 = new Parameter(null, "");
    parameter2 = new Parameter(null, "");

    assertEquals(parameter1.hashCode(), parameter2.hashCode());
    assertNotSame(parameter1, parameter2);

    parameter1 = new Parameter(null, null);
    parameter2 = new Parameter(null, null);

    assertEquals(parameter1.hashCode(), parameter2.hashCode());
    assertNotSame(parameter1, parameter2);
  }

  @Test
  public void testGetName() throws Exception {
    Parameter parameter = new Parameter(null, "value");

    assertNull(parameter.getKey());

    parameter = new Parameter("", "value");

    assertEquals("", parameter.getKey());

    parameter = new Parameter("key", "value");

    assertEquals("key", parameter.getKey());
  }

  @Test
  public void testGetValue() throws Exception {
    Parameter parameter = new Parameter("key", null);

    assertNull(parameter.getValue());

    parameter = new Parameter("key", "");

    assertEquals("", parameter.getValue());

    parameter = new Parameter("key", "value");

    assertEquals("value", parameter.getValue());
  }
}
