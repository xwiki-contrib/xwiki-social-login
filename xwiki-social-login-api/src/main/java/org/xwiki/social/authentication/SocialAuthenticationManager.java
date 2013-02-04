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
package org.xwiki.social.authentication;

import java.util.Map;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;

/**
 * Orchestrator of all things social authentication, it provides facilities to establish connections between XWiki users
 * and third party OAuth end-points, and to query both persistent and session status of such connections.
 */
@ComponentRole
public interface SocialAuthenticationManager
{
    /**
     * @param provider the provider to check if a user exists for or not. Examples: "facebook", "twitter", etc.
     * @param id the third party id to check if a user exists for or not. For the facebook provider, this would
     *            correspond to a FB id, and so on.
     * @return <code>true</code> if a user exists in the wiki for this provider and id, <code>false</code> otherwise. A
     *         user exists for a provider and id when it has a <tt>XWiki.SocialLoginProfileClass</tt> object in its
     *         profile page with matching provider and id properties
     */
    boolean userExists(String provider, String id);

    /**
     * @param provider the provider to lookup the user for. Examples: "facebook", "twitter", etc.
     * @param id the third party id to lookup the user for. For the facebook provider, this would correspond to a FB id,
     *            and so on.
     * @return the document reference for the user with a social profile that match the passed provider and id.
     *         <code>null</code> if no user have a social profile that matches those provider and id
     * @see #userExists(String, String)
     */
    DocumentReference getUser(String provider, String id);

    /**
     * @param user the reference to the document of the user to check for the presence of a certain provider
     * @param provider the provider to check the presence of
     * @return <code>true</code> if the user has a social profile for this provider in its XWiki profile,
     *         <code>false</code> otherwise
     */
    boolean hasProvider(DocumentReference user, String provider);

    /**
     * @return the current social authentication session, if it exists, <code>null</code> otherwise.
     */
    SocialAuthSession getSession();

    /**
     * @return <code>true</code> if the context user has a social session with any provider successfully connected,
     *         <code>false</code> otherwise.
     */
    boolean isConnected();

    /**
     * @param provider the provider to check the connection of
     * @return <code>true</code> if the context user has a social session with the specified provider connected,
     *         <code>false</code> otherwise.
     */
    boolean isConnected(String provider);

    /**
     * Requests an attempt at connecting with a social auth (OAuth) end point. This redirects to a third-party OAuth
     * end-point to verify authorization. If the user has already granted permission for our application, then it
     * redirects back to our site directly (with authorization payload), otherwise it requires the user to grant this
     * authorization, before redirecting back.
     * 
     * @param provider the provider to request the connection with
     * @param returnUrl the URL to provide as a return URL from the social site/OAuth end-point.
     * @throws SocialAuthException when something does not work as expected.
     */
    void requestConnection(String provider, String returnUrl) throws SocialAuthException;

    /**
     * Verifies third-party authorization payload, and tries to find a matching user in our XWiki user base. According
     * to configuration, this user might be created automatically he doesn't exist yet. Once the verification is OK and
     * a user if found, the social session is updated accordingly.
     * 
     * @param requestParameters the request parameters used to verify the authorization
     * @return the reference of the XWiki user matched or created, <code>null</code> when no such user could be matched
     * @throws SocialAuthException when something goes bananas
     */
    DocumentReference connect(Map<String, String> requestParameters) throws SocialAuthException;

    /**
     * Ensures a social session exists and is connected for the context user for the passed provider. This assumes the
     * user already has a profile defined for the provider in its XWiki user profile.
     * 
     * @param provider the provider to ensure connection for
     * @throws SocialAuthException when something goes wrong
     */
    void ensureConnected(String provider) throws SocialAuthException;

    /**
     * Creates a new user based on a social session already established.
     * 
     * @param extraProperties the extra user properties to create the user with (XWiki.XWikiUsers properties)
     * @return the reference of the document of the user created
     * @throws XWikiException when something doesn't work at the XWiki level (database, etc.)
     * @throws SocialAuthException when something doesn't work at this module's level (empty social session, etc.)
     */
    DocumentReference createUser(Map<String, String> extraProperties) throws XWikiException, SocialAuthException;

    /**
     * Creates a new user based on a social session already established, with a specific username.
     * 
     * @param username the username to create the user with
     * @param extraProperties the extra user properties to create the user with (XWiki.XWikiUsers properties)
     * @return the reference of the document of the user created
     * @throws XWikiException when something doesn't work at the XWiki level (database, etc.)
     * @throws SocialAuthException when something doesn't work at this module's level (empty social session, etc.)
     */
    DocumentReference createUser(String username, Map<String, String> extraProperties) throws XWikiException,
        SocialAuthException;

    /**
     * Tries to add a new social profile with to an existing XWiki user.
     * 
     * @param provider the provider to try to add the social profile for. Examples: "facebook", "twitter", etc.
     * @throws SocialAuthException when something goes wrong
     */
    void associateAccount(String provider) throws SocialAuthException;
}
