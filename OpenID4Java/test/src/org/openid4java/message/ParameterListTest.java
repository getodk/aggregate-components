/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
public class ParameterListTest {
  private ParameterList _parameterList;

  @Before
  public void setUp() throws Exception {
    _parameterList = new ParameterList();

    _parameterList.set(new Parameter("key1", "value1"));
    _parameterList.set(new Parameter("key1", "value2"));
    _parameterList.set(new Parameter("key2", "value1"));
  }

  @After
  public void tearDown() throws Exception {
    _parameterList = null;
  }

  @Test
  public void testEquals() throws Exception {
    ParameterList parameterList2 = new ParameterList();

    parameterList2.set(new Parameter("key1", "value1"));
    parameterList2.set(new Parameter("key1", "value2"));
    parameterList2.set(new Parameter("key2", "value1"));

    assertEquals(_parameterList, parameterList2);
    assertNotSame(_parameterList, parameterList2);

    parameterList2 = new ParameterList();

    parameterList2.set(new Parameter("key2", "value1"));
    parameterList2.set(new Parameter("key1", "value1"));
    parameterList2.set(new Parameter("key1", "value2"));
    parameterList2.set(new Parameter("key3", "value1"));
    parameterList2.set(new Parameter("key3", "value2"));
    parameterList2.set(new Parameter("key3", "value1"));
    parameterList2.removeParameters("key3");

    assertEquals(_parameterList, parameterList2);
    assertNotSame(_parameterList, parameterList2);

    parameterList2 = new ParameterList();

    // null not supported in compareTo()
    // parameterList2.set(new Parameter(null, null));
    // parameterList2.set(new Parameter(null, ""));
    // parameterList2.set(new Parameter("", null));
    parameterList2.set(new Parameter("", ""));
  }

  @Test
  public void testHashCode() throws Exception {
    ParameterList parameterList2 = new ParameterList();

    parameterList2.set(new Parameter("key1", "value1"));
    parameterList2.set(new Parameter("key1", "value2"));
    parameterList2.set(new Parameter("key2", "value1"));

    assertEquals(_parameterList.hashCode(), parameterList2.hashCode());
    assertNotSame(_parameterList, parameterList2);

    parameterList2 = new ParameterList();

    parameterList2.set(new Parameter("key2", "value1"));
    parameterList2.set(new Parameter("key1", "value1"));
    parameterList2.set(new Parameter("key1", "value2"));
    parameterList2.set(new Parameter("key3", "value1"));
    parameterList2.set(new Parameter("key3", "value2"));
    parameterList2.set(new Parameter("key3", "value1"));
    parameterList2.removeParameters("key3");

    assertEquals(_parameterList.hashCode(), parameterList2.hashCode());
    assertNotSame(_parameterList, parameterList2);
  }

  @Test
  public void testCopyConstructor() {
    ParameterList parameterList2 = new ParameterList(_parameterList);

    assertEquals(2, _parameterList.getParameters().size());
    assertEquals(2, parameterList2.getParameters().size());

    _parameterList.removeParameters("key1");

    assertEquals(1, _parameterList.getParameters().size());
    assertEquals(2, parameterList2.getParameters().size());
  }

  @Test
  public void testAdd() throws Exception {
    assertEquals(2, _parameterList.getParameters().size());

    _parameterList.set(new Parameter("key3", "value1"));

    assertEquals(3, _parameterList.getParameters().size());
  }

  @Test
  public void testGetParameter() throws Exception {
    Parameter parameter = _parameterList.getParameter("key2");

    assertNotNull(parameter);
    assertEquals("value1", parameter.getValue());
  }

  @Test
  public void testGetParameterNull() throws Exception {
    Parameter parameter = _parameterList.getParameter("key3");

    assertNull(parameter);
  }

  @Test
  public void testGetParameterValue() throws Exception {
    String value = _parameterList.getParameterValue("key2");

    assertNotNull(value);
    assertEquals("value1", value);
  }

  @Test
  public void testGetParameters() throws Exception {
    List<Parameter> parameters = _parameterList.getParameters();

    assertEquals(2, parameters.size());
  }

  @Test
  public void testGetParameters1Null() throws Exception {
    assertNull(_parameterList.getParameterValue("key3"));
  }

  @Test
  public void testRemoveParameters() throws Exception {
    _parameterList.removeParameters("key1");

    assertEquals(1, _parameterList.getParameters().size());

    _parameterList.removeParameters("key2");

    assertEquals(0, _parameterList.getParameters().size());
  }

  @Test
  public void testReplaceParameters() throws Exception {
    _parameterList.set(new Parameter("key2", "value3"));

    assertEquals("value3", _parameterList.getParameter("key2").getValue());
  }

  @Test
  public void testHasParameter() throws Exception {
    assertTrue(_parameterList.hasParameter("key1"));
    assertTrue(_parameterList.hasParameter("key2"));

    assertFalse(_parameterList.hasParameter("key3"));
  }

  @Test
  public void testCreateFromQueryString() throws Exception {
    ParameterList createdParameterList = ParameterList
        .createFromQueryString("key1=value%31&key1=value2&key2=value1");

    assertEquals(_parameterList, createdParameterList);

    createdParameterList = ParameterList.createFromQueryString("key1=value%31&key1=&key2=value1");

    assertEquals("", ((Parameter) createdParameterList.getParameters().get(0)).getValue());

    createdParameterList = ParameterList.createFromQueryString("key1=value%31&key1=&key2=");

    assertEquals("", createdParameterList.getParameterValue("key2"));
  }

  @Test
  public void testCreateFromKeyValueForm() throws Exception {
    ParameterList createdParameterList = ParameterList
        .createFromKeyValueForm("key1:value1\nkey1:value2\nkey2:value1");

    assertEquals(_parameterList, createdParameterList);

    createdParameterList = ParameterList.createFromKeyValueForm("key1:value1\nkey1:\nkey2:value1");

    assertEquals("", ((Parameter) createdParameterList.getParameters().get(0)).getValue());

    createdParameterList = ParameterList.createFromKeyValueForm("key1:value1\nkey1:\nkey2:");

    assertEquals("", createdParameterList.getParameterValue("key2"));

    createdParameterList = ParameterList.createFromKeyValueForm("key1:value1\nkey2:value:2");

    assertEquals("value:2", createdParameterList.getParameterValue("key2"));

    createdParameterList = ParameterList.createFromKeyValueForm("key1:value1\nkey2:value2\n");

    assertEquals("value2", createdParameterList.getParameterValue("key2"));
  }
}
