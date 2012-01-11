/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
public class MessageTest {
  private Message _msg;

  @Before
  public void setUp() throws Exception {
    ParameterList params = new ParameterList();

    params.set(new Parameter("key1", "value1"));
    params.set(new Parameter("key1", "value2"));
    params.set(new Parameter("key2", "value1"));

    _msg = new Message(params);
  }

  @After
  public void tearDown() throws Exception {
    _msg = null;
  }

  @Test
  public void testKeyValueFormEncoding() throws Exception {
    String keyValueForm = "key1:value2\nkey2:value1\n";

    assertEquals(keyValueForm, _msg.keyValueFormEncoding());
  }

  @Test
  public void testWwwFormEncoding() throws Exception {
    String wwwForm = "openid.key1=value2&openid.key2=value1";

    assertEquals(wwwForm, _msg.wwwFormEncoding());
  }

  @Test
  public void testNotAllowedChars() throws Exception {
    Parameter param;
    Map<String, String> parameterMap;

    try {
      // semicolon in key
      param = new Parameter("some:key", "value");
      parameterMap = new HashMap<String, String>();
      parameterMap.put(param.getKey(), param.getValue());

      Message.createMessage(new ParameterList(parameterMap));

      fail("A MessageException should be thrown " + "if the key/values contain invalid characters");
    } catch (MessageException expected) {
      assertTrue(true);
    }
    try {
      // newline in key
      param = new Parameter("some\nkey\n", "value");
      parameterMap = new HashMap<String, String>();
      parameterMap.put(param.getKey(), param.getValue());

      Message.createMessage(new ParameterList(parameterMap));

      fail("A MessageException should be thrown " + "if the key/values contain invalid characters");
    } catch (MessageException expected) {
      assertTrue(true);
    }
    try {
      // newline in value
      param = new Parameter("key", "val\nue");
      parameterMap = new HashMap<String, String>();
      parameterMap.put(param.getKey(), param.getValue());

      Message.createMessage(new ParameterList(parameterMap));

      fail("A MessageException should be thrown " + "if the key/values contain invalid characters");
    } catch (MessageException expected) {
      assertTrue(true);
    }
    try {
      // all of the above
      param = new Parameter("some:\nkey", "value\n");
      parameterMap = new HashMap<String, String>();
      parameterMap.put(param.getKey(), param.getValue());

      Message.createMessage(new ParameterList(parameterMap));

      fail("A MessageException should be thrown " + "if the key/values contain invalid characters");
    } catch (MessageException expected) {
      assertTrue(true);
    }
  }

}
