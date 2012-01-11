/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.util;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
public class InternetDateFormatTest {
  InternetDateFormat _dateFormat;

  @Before
  public void setUp() throws Exception {
    _dateFormat = new InternetDateFormat();
  }

  @Test
  public void testFormat() {
    Date date0 = new Date(0);

    assertEquals("1970-01-01T00:00:00Z", _dateFormat.format(date0));
  }

  @Test
  public void testParse() throws ParseException {
    Date date0 = new Date(0);

    assertEquals(date0, _dateFormat.parse("1970-01-01T00:00:00Z"));
    assertEquals(date0, _dateFormat.parse("1970-01-01t00:00:00z"));
  }
}
