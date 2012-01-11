/*
 * Copyright 2006-2008 Sxip Identity Corporation
 */

package org.openid4java.consumer;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.crypto.spec.DHParameterSpec;

import org.openid4java.association.AssociationException;
import org.openid4java.association.AssociationSessionType;
import org.openid4java.discovery.Discovery;
import org.openid4java.discovery.DiscoveryException;
import org.openid4java.discovery.DiscoveryInformation;
import org.openid4java.message.AuthRequest;
import org.openid4java.message.AuthSuccess;
import org.openid4java.message.MessageException;
import org.openid4java.message.ParameterList;
import org.openid4java.server.RealmVerifier;
import org.openid4java.util.HttpFetcher;

import com.google.inject.ImplementedBy;
import com.google.inject.Inject;

/**
 * Manages OpenID communications with an OpenID Provider (Server).
 * <p>
 * The Consumer site needs to have the same instance of this class throughout
 * the lifecycle of a OpenID authentication session.
 * 
 * Revised to be a pure interface to decouple external users of this 
 * class from the implementation.  Also added a ConsumerManagerFactory
 * to support non-Juice consumers of this class.
 *
 * @author Marius Scurtescu, Johnny Bufu
 * @author mitchellsundt@gmail.com
 */
@ImplementedBy(ConsumerManagerImpl.class)
public interface ConsumerManager
{
    /**
     * Returns discovery process manager.
     *
     * @return discovery process manager.
     */
    public Discovery getDiscovery();

    /**
     * Sets discovery process manager.
     *
     * @param discovery discovery process manager.
     */
    public void setDiscovery(Discovery discovery);

    /**
     * Gets the association store that holds established associations with
     * OpenID providers.
     *
     * @see ConsumerAssociationStore
     */
    public ConsumerAssociationStore getAssociations();

    /**
     * Configures the ConsumerAssociationStore that will be used to store the
     * associations established with OpenID providers.
     *
     * @param associations              ConsumerAssociationStore implementation
     * @see ConsumerAssociationStore
     */
    @Inject
    public void setAssociations(ConsumerAssociationStore associations);

    /**
     * Gets the NonceVerifier implementation used to keep track of the nonces
     * that have been seen in authentication response messages.
     *
     * @see NonceVerifier
     */
    public NonceVerifier getNonceVerifier();

    /**
     * Configures the NonceVerifier that will be used to keep track of the
     * nonces in the authentication response messages.
     *
     * @param nonceVerifier         NonceVerifier implementation
     * @see NonceVerifier
     */
    @Inject
    public void setNonceVerifier(NonceVerifier nonceVerifier);

    /**
     * Sets the Diffie-Hellman base parameters that will be used for encoding
     * the MAC key exchange.
     * <p>
     * If not provided the default set specified by the Diffie-Hellman algorithm
     * will be used.
     *
     * @param dhParams      Object encapsulating modulus and generator numbers
     * @see DHParameterSpec DiffieHellmanSession
     */
    public void setDHParams(DHParameterSpec dhParams);

    /**
     * Gets the Diffie-Hellman base parameters (modulus and generator).
     *
     * @see DHParameterSpec DiffieHellmanSession
     */
    public DHParameterSpec getDHParams();

    /**
     * Maximum number of attempts (HTTP calls) the RP is willing to make
     * for trying to establish an association with the OP.
     *
     * Default: 4;
     * 0 = don't use associations
     *
     * Associations and stateless mode cannot be both disabled at the same time.
     */
    public void setMaxAssocAttempts(int maxAssocAttempts);

    /**
     * Gets the value configured for the maximum number of association attempts
     * that will be performed for a given OpenID provider.
     * <p>
     * If an association cannot be established after this number of attempts the
     * ConsumerManager will fallback to stateless mode, provided the
     * #allowStateless preference is enabled.
     * <p>
     * See also: {@link #allowStateless(boolean)} {@link #statelessAllowed()}
     */
    public int getMaxAssocAttempts();

    /**
     * Flag used to enable / disable the use of stateless mode.
     * <p>
     * Default: enabled.
     * <p>
     * Associations and stateless mode cannot be both disabled at the same time.
     */
    public void setAllowStateless(boolean allowStateless);

    /**
     * Returns true if the ConsumerManager is configured to fallback to
     * stateless mode when failing to associate with an OpenID Provider.
     */
    public boolean isAllowStateless();

    /**
     * Configures the minimum level of encryption accepted for association
     * sessions.
     * <p>
     * Default: no-encryption session, SHA1 MAC association.
     * <p>
     * See also: {@link #allowStateless(boolean)}
     */
    public void setMinAssocSessEnc(AssociationSessionType minAssocSessEnc);

    /**
     * Gets the minimum level of encryption that will be accepted for
     * association sessions.
     * <p>
     * Default: no-encryption session, SHA1 MAC association
     * <p>
     */
    public AssociationSessionType getMinAssocSessEnc();

    /**
     * Sets the preferred encryption type for the association sessions.
     * <p>
     * Default: DH-SHA256
     */
    public void setPrefAssocSessEnc(AssociationSessionType prefAssocSessEnc);

    /**
     * Gets the preferred encryption type for the association sessions.
     */
    public AssociationSessionType getPrefAssocSessEnc();

    /**
     * Sets the expiration timeout (in seconds) for keeping track of failed
     * association attempts.
     * <p>
     * If an association cannot be establish with an OP, subsequesnt
     * authentication request to that OP will not try to establish an
     * association within the timeout period configured here.
     * <p>
     * Default: 300s
     * 0 = disabled (attempt to establish an association with every
     *               authentication request)
     *
     * @param _failedAssocExpire    time in seconds to remember failed
     *                              association attempts
     */
    public void setFailedAssocExpire(int _failedAssocExpire);

    /**
     * Gets the timeout (in seconds) configured for keeping track of failed
     * association attempts.
     * <p>
     * See also: {@link #setFailedAssocExpire(int)}
     */
    public int getFailedAssocExpire();

    /**
     * Gets the interval before the expiration of an association
     * (in seconds) in which the association should not be used,
     * in order to avoid the expiration from occurring in the middle
     * of a authentication transaction. Default: 300s.
     */
    public int getPreExpiryAssocLockInterval();

    /**
     * Sets the interval before the expiration of an association
     * (in seconds) in which the association should not be used,
     * in order to avoid the expiration from occurring in the middle
     * of a authentication transaction. Default: 300s.
     *
     * @param preExpiryAssocLockInterval    The number of seconds for the
     *                                      pre-expiry lock inteval.
     */
    public void setPreExpiryAssocLockInterval(int preExpiryAssocLockInterval);

    /**
     * Configures the authentication request mode:
     * checkid_immediate (true) or checkid_setup (false).
     * <p>
     * Default: false / checkid_setup
     */
    public void setImmediateAuth(boolean _immediateAuth);

    /**
     * Returns true if the ConsumerManager is configured to attempt
     * checkid_immediate authentication requests.
     * <p>
     * Default: false
     */
    public boolean isImmediateAuth();

    /**
     * Gets the RealmVerifier used to verify realms against return_to URLs.
     */
    public RealmVerifier getRealmVerifier();

    /**
     * Sets the RealmVerifier used to verify realms against return_to URLs.
     */
    public void setRealmVerifier(RealmVerifier realmVerifier);

    /**
     * Gets the max age (in seconds) configured for keeping track of nonces.
     * <p>
     * Nonces older than the max age will be removed from the store and
     * authentication responses will be considered failures.
     */
    public int getMaxNonceAge();

    /**
     * Sets the max age (in seconds) configured for keeping track of nonces.
     * <p>
     * Nonces older than the max age will be removed from the store and
     * authentication responses will be considered failures.
     */
    public void setMaxNonceAge(int ageSeconds);

    /**
     * Does discovery on an identifier. It delegates the call to its
     * discovery manager.
     *
     * @return      A List of {@link DiscoveryInformation} objects.
     *              The list could be empty if no discovery information can
     *              be retrieved.
     *
     * @throws DiscoveryException if the discovery process runs into errors.
     */
    public List<DiscoveryInformation> discover(String identifier) throws DiscoveryException;

    /**
     * Configures a private association store for signing consumer nonces.
     * <p>
     * Consumer nonces are needed to prevent replay attacks in compatibility
     * mode, because OpenID 1.x Providers to not attach nonces to
     * authentication responses.
     * <p>
     * One way for the Consumer to know that a consumer nonce in an
     * authentication response was indeed issued by itself (and thus prevent
     * denial of service attacks), is by signing them.
     *
     * @param associations     The association store to be used for signing consumer nonces;
     *                  signing can be deactivated by setting this to null.
     *                  Signing is enabled by default.
     */
    public void setPrivateAssociationStore(ConsumerAssociationStore associations)
            throws ConsumerException;

    /**
     * Gets the private association store used for signing consumer nonces.
     *
     * @see #setPrivateAssociationStore(ConsumerAssociationStore)
     */
    public ConsumerAssociationStore getPrivateAssociationStore();

    public void setConnectTimeout(int connectTimeout);

    public void setSocketTimeout(int socketTimeout);

    public void setMaxRedirects(int maxRedirects);

    /**
     * Tries to establish an association with on of the service endpoints in
     * the list of DiscoveryInformation.
     * <p>
     * Iterates over the items in the discoveries parameter a maximum of
     * #_maxAssocAttempts times trying to esablish an association.
     *
     * @param discoveries       The DiscoveryInformation list obtained by
     *                          performing dicovery on the User-supplied OpenID
     *                          identifier. Should be ordered by the priority
     *                          of the service endpoints.
     * @return                  The DiscoveryInformation instance with which
     *                          an association was established, or the one
     *                          with the highest priority if association failed.
     *
     * @see Discovery#discover(org.openid4java.discovery.Identifier)
     */
    public DiscoveryInformation associate(List<DiscoveryInformation> discoveries);

    /**
     * Builds a authentication request message for the user specified in the
     * discovery information provided as a parameter.
     * <p>
     * If the discoveries parameter contains more than one entry, it will
     * iterate over them trying to establish an association. If an association
     * cannot be established, the first entry is used with stateless mode.
     *
     * @see #associate(java.util.List)
     * @param discoveries       The DiscoveryInformation list obtained by
     *                          performing dicovery on the User-supplied OpenID
     *                          identifier. Should be ordered by the priority
     *                          of the service endpoints.
     * @param returnToUrl       The URL on the Consumer site where the OpenID
     *                          Provider will return the user after generating
     *                          the authentication response. <br>
     *                          Null if the Consumer does not with to for the
     *                          End User to be returned to it (something else
     *                          useful will have been performed via an
     *                          extension). <br>
     *                          Must not be null in OpenID 1.x compatibility
     *                          mode.
     * @return                  Authentication request message to be sent to the
     *                          OpenID Provider.
     */
    public AuthRequest authenticate(List<DiscoveryInformation> discoveries,
                                    String returnToUrl)
            throws ConsumerException, MessageException;

    /**
     * Builds a authentication request message for the user specified in the
     * discovery information provided as a parameter.
     * <p>
     * If the discoveries parameter contains more than one entry, it will
     * iterate over them trying to establish an association. If an association
     * cannot be established, the first entry is used with stateless mode.
     *
     * @see #associate(java.util.List)
     * @param discoveries       The DiscoveryInformation list obtained by
     *                          performing dicovery on the User-supplied OpenID
     *                          identifier. Should be ordered by the priority
     *                          of the service endpoints.
     * @param returnToUrl       The URL on the Consumer site where the OpenID
     *                          Provider will return the user after generating
     *                          the authentication response. <br>
     *                          Null if the Consumer does not with to for the
     *                          End User to be returned to it (something else
     *                          useful will have been performed via an
     *                          extension). <br>
     *                          Must not be null in OpenID 1.x compatibility
     *                          mode.
     * @param realm             The URL pattern that will be presented to the
     *                          user when he/she will be asked to authorize the
     *                          authentication transaction. Must be a super-set
     *                          of the @returnToUrl.
     * @return                  Authentication request message to be sent to the
     *                          OpenID Provider.
     */
    public AuthRequest authenticate(List<DiscoveryInformation> discoveries,
                                    String returnToUrl, String realm)
            throws ConsumerException, MessageException;

    /**
     * Builds a authentication request message for the user specified in the
     * discovery information provided as a parameter.
     *
     * @param discovered        A DiscoveryInformation endpoint from the list
     *                          obtained by performing dicovery on the
     *                          User-supplied OpenID identifier.
     * @param returnToUrl       The URL on the Consumer site where the OpenID
     *                          Provider will return the user after generating
     *                          the authentication response. <br>
     *                          Null if the Consumer does not with to for the
     *                          End User to be returned to it (something else
     *                          useful will have been performed via an
     *                          extension). <br>
     *                          Must not be null in OpenID 1.x compatibility
     *                          mode.
     * @return                  Authentication request message to be sent to the
     *                          OpenID Provider.
     */
    public AuthRequest authenticate(DiscoveryInformation discovered,
                                    String returnToUrl)
            throws MessageException, ConsumerException;

    /**
     * Builds a authentication request message for the user specified in the
     * discovery information provided as a parameter.
     *
     * @param discovered        A DiscoveryInformation endpoint from the list
     *                          obtained by performing dicovery on the
     *                          User-supplied OpenID identifier.
     * @param returnToUrl       The URL on the Consumer site where the OpenID
     *                          Provider will return the user after generating
     *                          the authentication response. <br>
     *                          Null if the Consumer does not with to for the
     *                          End User to be returned to it (something else
     *                          useful will have been performed via an
     *                          extension). <br>
     *                          Must not be null in OpenID 1.x compatibility
     *                          mode.
     * @param realm             The URL pattern that will be presented to the
     *                          user when he/she will be asked to authorize the
     *                          authentication transaction. Must be a super-set
     *                          of the @returnToUrl.
     * @return                  Authentication request message to be sent to the
     *                          OpenID Provider.
     */
    public AuthRequest authenticate(DiscoveryInformation discovered,
                                    String returnToUrl, String realm)
            throws MessageException, ConsumerException;

    /**
     * Performs verification on the Authentication Response (assertion)
     * received from the OpenID Provider.
     * <p>
     * Three verification steps are performed:
     * <ul>
     * <li> nonce:                  the same assertion will not be accepted more
     *                              than once
     * <li> signatures:             verifies that the message was indeed sent
     *                              by the OpenID Provider that was contacted
     *                              earlier after discovery
     * <li> discovered information: the information contained in the assertion
     *                              matches the one obtained during the
     *                              discovery (the OpenID Provider is
     *                              authoritative for the claimed identifier;
     *                              the received assertion is not meaningful
     *                              otherwise
     * </ul>
     *
     * @param receivingUrl  The URL where the Consumer (Relying Party) has
     *                      accepted the incoming message.
     * @param response      ParameterList of the authentication response
     *                      being verified.
     * @param discovered    Previously discovered information (which can
     *                      therefore be trusted) obtained during the discovery
     *                      phase; this should be stored and retrieved by the RP
     *                      in the user's session.
     *
     * @return              A VerificationResult, containing a verified
     *                      identifier; the verified identifier is null if
     *                      the verification failed).
     */
    public VerificationResult verify(String receivingUrl,
                                     ParameterList response,
                                     DiscoveryInformation discovered)
            throws MessageException, DiscoveryException, AssociationException;

    /**
     * Verifies that the URL where the Consumer (Relying Party) received the
     * authentication response matches the value of the "openid.return_to"
     * parameter in the authentication response.
     *
     * @param receivingUrl      The URL where the Consumer received the
     *                          authentication response.
     * @param response          The authentication response.
     * @return                  True if the two URLs match, false otherwise.
     */
    public boolean verifyReturnTo(String receivingUrl, AuthSuccess response);

    /**
     * Returns a Map(key, List(values)) with the URL's query params, or null if
     * the URL doesn't have a query string.
     */
    public Map<String,List<String>> extractQueryParams(URL url) throws UnsupportedEncodingException;

    /**
     * Verifies the nonce in an authentication response.
     *
     * @param authResp      The authentication response containing the nonce
     *                      to be verified.
     * @param discovered    The discovery information associated with the
     *                      authentication transaction.
     * @return              True if the nonce is valid, false otherwise.
     */
    public boolean verifyNonce(AuthSuccess authResp,
                               DiscoveryInformation discovered);

    /**
     * Inserts a consumer-side nonce as a custom parameter in the return_to
     * parameter of the authentication request.
     * <p>
     * Needed for preventing replay attack when running compatibility mode.
     * OpenID 1.1 OpenID Providers do not generate nonces in authentication
     * responses.
     *
     * @param opUrl             The endpoint to be used for private association.
     * @param returnTo          The return_to URL to which a custom nonce
     *                          parameter will be added.
     * @return                  The return_to URL containing the nonce.
     */
    public String insertConsumerNonce(String opUrl, String returnTo);

    /**
     * Extracts the consumer-side nonce from the return_to parameter in
     * authentication response from a OpenID 1.1 Provider.
     *
     * @param returnTo      return_to URL from the authentication response
     * @param opUrl         URL for the appropriate OP endpoint
     * @return              The nonce found in the return_to URL, or null if
     *                      it wasn't found.
     */
    public String extractConsumerNonce(String returnTo, String opUrl);

    /* visible for testing */
    public HttpFetcher getHttpFetcher();
}
