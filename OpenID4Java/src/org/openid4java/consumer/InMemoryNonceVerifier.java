/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.text.ParseException;

/**
 * @author Marius Scurtescu, Johnny Bufu
 */
public class InMemoryNonceVerifier extends AbstractNonceVerifier
{
    private static Log _log = LogFactory.getLog(InMemoryNonceVerifier.class);
    private static final boolean DEBUG = _log.isDebugEnabled();

    private Map<String,Set<String>> _opMap = new HashMap<String,Set<String>>();

    public InMemoryNonceVerifier() {
      this(60);
    }

    public InMemoryNonceVerifier(int maxAge)
    {
        super(maxAge);
    }

    protected synchronized int seen(Date now, String opUrl, String nonce)
    {
        removeAged(now);

        Set<String> seenSet = _opMap.get(opUrl);

        if (seenSet == null)
        {
            seenSet = new HashSet<String>();

            _opMap.put(opUrl, seenSet);
        }

        if (seenSet.contains(nonce))
        {
            _log.error("Possible replay attack! Already seen nonce: " + nonce);
            return SEEN;
        }

        seenSet.add(nonce);

        if (DEBUG) _log.debug("Nonce verified: " + nonce);

        return OK;
    }

    private synchronized void removeAged(Date now)
    {
        Set<String> opToRemove = new HashSet<String>();
        Iterator<String> opUrls = _opMap.keySet().iterator();
        while (opUrls.hasNext())
        {
            String opUrl = opUrls.next();

            Set<String> seenSet = _opMap.get(opUrl);
            Set<String> nonceToRemove = new HashSet<String>();

            Iterator<String> nonces = seenSet.iterator();
            while (nonces.hasNext())
            {
                String nonce = nonces.next();

                try
                {
                    Date nonceDate = _dateFormat.parse(nonce);

                    if (isTooOld(now, nonceDate))
                    {
                        nonceToRemove.add(nonce);
                    }
                }
                catch (ParseException e)
                {
                    nonceToRemove.add(nonce);
                }
            }

            nonces = nonceToRemove.iterator();
            while (nonces.hasNext())
            {
                String nonce = nonces.next();

                if (DEBUG)
                    _log.debug("Removing nonce: " + nonce +
                               " from OP: " + opUrl);
                seenSet.remove(nonce);
            }

            if (seenSet.size() == 0)
                opToRemove.add(opUrl);
        }

        opUrls = opToRemove.iterator();
        while (opUrls.hasNext())
        {
            String opUrl = opUrls.next();

            if (DEBUG) _log.debug("Removed all nonces from OP: " + opUrl);

            _opMap.remove(opUrl);
        }
    }

    protected synchronized int size()
    {
        int total = 0;

        Iterator<String> opUrls = _opMap.keySet().iterator();
        while (opUrls.hasNext())
        {
            String opUrl = opUrls.next();

            Set<String> seenSet = _opMap.get(opUrl);

            total += seenSet.size();
        }

        return total;
    }
}
