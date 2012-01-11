/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.server;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.openid4java.util.InternetDateFormat;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
public abstract class AbstractNonceGeneratorTest
{
    protected InternetDateFormat _dateFormat = new InternetDateFormat();
    protected NonceGenerator _nonceGenerator;

    public AbstractNonceGeneratorTest()
    {
    }

    @Before
    public void setUp() throws Exception
    {
        _nonceGenerator = createGenerator();
    }

    public abstract NonceGenerator createGenerator();

    @Test
    public void testUniqueLoop()
    {
        Set<String> seen = new HashSet<String>();

        for (int i = 0; i < 100; i++)
        {
            String nonce = _nonceGenerator.next();

            if (seen.contains(nonce))
                fail("Double nonce!");

            seen.add(nonce);
        }
    }

    @Test
    public void testUniqueSequential()
    {
        String nonce1 = _nonceGenerator.next();
        String nonce2 = _nonceGenerator.next();
        String nonce3 = _nonceGenerator.next();

        assertFalse(nonce1.equals(nonce2));
        assertFalse(nonce2.equals(nonce3));
    }

    @Test
    public void testTimestamp() throws ParseException
    {
        String nonce = _nonceGenerator.next();

        Date nonceDate = _dateFormat.parse(nonce);

        assertNotNull(nonceDate);
        assertTrue(nonceDate.before(new Date()));
    }
}
