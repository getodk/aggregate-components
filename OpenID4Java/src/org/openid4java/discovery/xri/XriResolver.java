package org.openid4java.discovery.xri;

import java.util.List;

import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.discovery.XriIdentifier;
import org.openid4java.util.HttpClientFactory;

import com.google.inject.ImplementedBy;

@ImplementedBy(XriDotNetProxyResolver.class)
public interface XriResolver
{
    /**
     * Set the client factory to be used by the resolver.
     * 
     * @param clientFactory
     */
    public void setHttpClientFactory(HttpClientFactory clientFactory);
    /**
     * Performs OpenID discovery on the supplied XRI identifier.
     *
     * @param xri   The XRI identifier
     * @return      A list of DiscoveryInformation, ordered the discovered
     *              priority.
     * @throws DiscoveryException if discovery failed.
     */
    public List<DiscoveryInformation> discover(XriIdentifier xri) throws DiscoveryException;

    XriIdentifier parseIdentifier(String identifier) throws DiscoveryException;
}
