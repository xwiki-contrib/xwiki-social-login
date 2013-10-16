/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.social.authentication.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.brickred.socialauth.util.SocialAuthUtil;
import org.securityfilter.realm.SimplePrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.crypto.passwd.PasswordCryptoService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.social.authentication.SocialAuthConfiguration;
import org.xwiki.social.authentication.SocialAuthConstants;
import org.xwiki.social.authentication.SocialAuthException;
import org.xwiki.social.authentication.SocialAuthSession;
import org.xwiki.social.authentication.SocialAuthenticationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl;
import com.xpn.xwiki.web.Utils;

/**
 * <p>
 * An authenticator for social networks/OAuth end-points, based on Redbrick SocialAuth. The authenticator allows
 * optional fall-back on XWiki authentication.
 * </p>
 * <p>
 * When authenticating, this authenticator ensures a successful OAuth handshake has been established, and that a XWiki
 * user matches the third-party user used against the OAuth endpoint. If no such user exists, then according to the
 * module's configuration, it is either created automatically, either with optional confirmation steps (such as the
 * confirmation of the username to use, the request of extra registration information, etc.).
 * </p>
 * <p>
 * Internally the authenticator works the following way :
 * <ul>
 * <li>OAuth authentication status is held in memory, in the user session (and is protected with an encrypted password
 * to prevent status forgery, this will be explained later).</li>
 * <li>When no such status exists and no OAuth handshake has been requested, this authenticator is not concerned. It
 * then either leave it to the XWiki authentication service to try and authenticate the request, or leave it
 * unauthenticated, according to configuration.</li>
 * <li>Authentication against a particular OAuth end-point (a.k.a a "provider") is considered "requested" when the
 * request contains a value for the parameter {@link SocialAuthConstants#PROVIDER_PARAMETER}. When such request is
 * expressed, and assuming the provider is configured properly, the authenticator redirects the user to the third party
 * site for authorization, providing a callback URL (or "return url"). If the authorization on the third party website
 * has been granted previously, then the third party web-site will redirect to the return URL directly, if not user
 * confirmation will be necessary to grant XWiki the authorization to act as a OAuth consumer. When returning from OAuth
 * (detected by the authenticator by the presence of the {@link SocialAuthConstants#CALLBACK_PARAMETER}, the
 * authenticator will verify the third party response. If it is valid, it means the OAuth handshake is successful.</li>
 * <li>A successful handshake is followed by the lookup of a XWiki user that has in its profile page information
 * matching the provider and the returned third party user ("profile id"). This information is looked up in an object of
 * class <tt>XWiki.SocialLoginProfileClasss</tt>. If such user exists, it means it's a returning user. The authenticator
 * then looks up a "password" string held in that <tt>XWiki.SocialLoginProfileClasss</tt> (which is a random string,
 * unknown to the user), encrypt it with a secret key, and stores it in the user session together in a
 * {@link SocialAuthSession} object. The authenticator then triggers another
 * {@link #authenticate(String, String, XWikiContext)} call, passing as username/password the name of the matched XWiki
 * document, and the retrieved password.
 * <li>When authentication is requested and a {@link SocialAuthSession} exists in the user session (potentially meaning
 * a successful handshake has previously been established), the passed password is compared against the one encrypted in
 * the session. If they match, the user is considered authenticated, and its "credentials" (which are unknown to the
 * user) are remembered using the standard XWiki persistent login mechanism (that uses encrypted cookies), so that
 * following requests continue to be successfully authenticated. If they don't match, no user is authenticated.</li>
 * <li>When a successful OAuth handshake is followed by a looked that finds no matching XWiki user, then the user can be
 * either created dynamically, or "manually" with extra confirmation steps, according to the configuration. When the
 * user is created, the same steps apply as for an returning user.</li>
 * </p>
 * <p>
 * Overall, session forgery is protected against via the fact successful OAuth handshakes are "stamped" with a
 * clear-text password encrypted with a secret key known only to the internal system. Stealing a user's "password"
 * (calling ?xpage=xml on its profile page for example) doesn't help to create the fake OAuth status in a session,
 * without knowing the private key).
 * </p>
 * <p>
 * See {@link SocialAuthConfiguration} for configuration options.
 * </p>
 * 
 * @version $Id$
 */
public class SocialAuthServiceImpl extends XWikiAuthServiceImpl implements SocialAuthConstants
{
    /**
     * Logger used for this authenticator.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SocialAuthServiceImpl.class);

    @Override
    public Principal authenticate(String login, String password, XWikiContext context) throws XWikiException
    {
        LOGGER.debug("Social login authenticate...");

        HttpServletRequest request = context.getRequest();
        HttpSession httpSession = request.getSession();
        SocialAuthenticationManager manager = Utils.getComponent(SocialAuthenticationManager.class);

        

        if (StringUtils.isBlank(request.getParameter(PROVIDER_PARAMETER)) && !manager.isConnected()) {
            // Passing along to XWiki authentication

            // TODO add a parameter in configuration to declare if normal XWiki auth is allowed
            // or not.

            LOGGER.debug("No provider given: let XWiki authenticate...");
            return super.authenticate(login, password, context);
        }

        SocialAuthSession session = manager.getSession();
        
        String provider =
            StringUtils.defaultIfBlank(session != null ? session.getCurrentProvider() : null,
                request.getParameter(PROVIDER_PARAMETER));

        try {
            if (!manager.isConnected(provider) || password == null) {
                // If no social session is present in the user session, we try to associate one for the request
                // provider by triggering a OAuth handshake. Note this is a two step process, meaning two consecutive
                // requests will have to go through this call.

                trySocialAuthConnect(provider, manager, context);
                return null;

            } else {
                // If a social auth session status is actually present in the user session, we validate the passed
                // password
                // which comes either from :
                // * a #checkAuth call triggered in #trySocialConnect
                // * a #authenticate call triggered by the persistent login manager when authenticating from cookies
                //
                // If password matches with the one stored (encrypted) in the session Fthen we validate the connection
                // returning the user principal. If the password doesn't come from cookies already, then the
                // authenticator will take care of remembering those credentials in cookies via the persistent login
                // manager.

                return validateCredentials(login, password, session, manager, context);
            }
        } catch (Exception e) {
            LOGGER.error("Error while Social login authentication", e);
        }
        return null;
    }

    /**
     * Initiates or validates an OAuth handshake.
     * 
     * @param provider the id of the provider to connect to. Example: "facebook", "twitter", etc.
     * @param manager the social authentication manager used to connect
     * @param context the XWiki context
     * @throws SocialAuthException when something goes wrong at the SocialAuth/OAuth level
     * @throws XWikiException when something goes wrong at the XWiki level
     */
    private void trySocialAuthConnect(String provider, SocialAuthenticationManager manager, XWikiContext context)
        throws SocialAuthException, XWikiException
    {
        HttpServletRequest request = context.getRequest();
        if (StringUtils.isBlank(request.getParameter(CALLBACK_PARAMETER)) && manager.getSession() == null) {
            // Step 1.
            // Redirect the request towards the target OAuth endpoint

            String url =
                request.getRequestURL() + "?" + CALLBACK_PARAMETER + "=1&" + PROVIDER_PARAMETER + "=" + provider;

            
            // FIXME Right now the xredirect parameter is lost because we don't pass the full query string
            // in the redirect_uri.
            // There is an issue with some special characters in the redirect_uri that will make Facebook
            // not validate the request.
            // See http://stackoverflow.com/questions/4386691/facebook-error-error-validating-verification-code
            
            //in case the provider is not Facebook, keep the xredirect parameter
            String redirect = context.getRequest().getParameter("xredirect");
            if (redirect != null && provider != null && !provider.toLowerCase().equals("facebook")) {
               try {
                   url = url + "&xredirect=" + URLEncoder.encode(redirect, "UTF-8");
               } catch (UnsupportedEncodingException e) {
                    throw new SocialAuthException("Bad URL encoding", e);
               }
            }


            manager.requestConnection(provider, url);
        } else {
            // Step 2.
            // Handle response from the OAuth endpoint

            DocumentReference user;
            if (manager.getSession() == null || manager.getSession().getProfile() == null) {
                LOGGER.debug("We back from OAuth URL");
                user = manager.connect(SocialAuthUtil.getRequestParametersMap(request));
            } else {
                user = manager.getUser(provider, manager.getSession().getProfile().getValidatedId());
                LOGGER.debug("Already a profile in the session. User " + user);
            }
            
            if (user != null) {
                XWikiDocument document = context.getWiki().getDocument(user, context);
                BaseObject profileObject = document.getObject(SOCIAL_LOGIN_PROFILE_CLASS, "provider", provider);
                String passwd = profileObject.getStringValue("password");

                // We have just associated a social profile with the user session, which contains an encrypted password.
                // Now we need to trigger the actual authentication and persistence of those credentials in cookies to
                // actually log the user in. The following checkAuth call does just that :
                // it will trigger a call to #authenticate to verify the (clear-text) password against the one encrypted
                // in the session, and if they match, it will return the proper principal and remember user credentials
                // via the persistence login manager so that next requests keep being properly authenticated.
                this.checkAuth(document.getName(), passwd, "true", context);
            }
        }
    }

    /**
     * Validates a username/password against a social profile (coming from memory, and with its password encrypted).
     * 
     * @param username the username to verify
     * @param password the password to verify
     * @param session the social auth session containing the profile to verify against, coming from memory
     * @param manager the social authentication manager, used here to retrieved a user from the profile
     * @param context the XWiki context
     * @return a principal if the test is successful, null otherwise
     * @throws GeneralSecurityException when something goes wrong decrypting the password in the profile
     * @throws XWikiException when something goes wrong at the XWiki level
     */
    private Principal validateCredentials(String username, String password, SocialAuthSession session,
        SocialAuthenticationManager manager, XWikiContext context) throws GeneralSecurityException, XWikiException
    {
        LOGGER.debug("Found a social profile in session");
        PasswordCryptoService passwordCryptoService = Utils.getComponent(PasswordCryptoService.class);
        String key = context.getWiki().Param("xwiki.authentication.encryptionKey");

        DocumentReference user =
            manager.getUser(session.getProfile().getProviderId(), session.getProfile().getValidatedId());
        XWikiDocument document = context.getWiki().getDocument(user, context);

        if (!StringUtils.isBlank(session.getEncryptedPassword())
            && passwordCryptoService.decryptText(session.getEncryptedPassword(), key).equals(password)
            && username.equals(document.getName())) {

            LOGGER.debug("Password match, returning principal " + document.getName());
            return new SimplePrincipal(document.getPrefixedFullName());
        }
        
        LOGGER.debug("Password null or password mismatch");
        
        if (!StringUtils.isBlank(password)) {
            LOGGER.debug("Password null");    
            context.getRequest().getSession().removeAttribute(SOCIAL_AUTH_SESSION_ATTRIBUTE);
        }
        // Leave to the XWiki authentication
        // TODO check a "trylocal" parameter simalar to LDAP auth
        return super.authenticate(username, password, context);
    }

}
