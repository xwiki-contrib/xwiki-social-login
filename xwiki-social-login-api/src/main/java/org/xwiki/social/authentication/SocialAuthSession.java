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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.brickred.socialauth.AuthProvider;
import org.brickred.socialauth.Profile;
import org.brickred.socialauth.SocialAuthManager;

public class SocialAuthSession
{
    private String encryptedPassword;

    private Map<String, AuthProvider> providers = new HashMap<String, AuthProvider>();

    private SocialAuthManager authManager;

    private String currentProvider;

    public SocialAuthSession(SocialAuthManager authManager)
    {
        this.authManager = authManager;
    }

    public SocialAuthManager getAuthManager()
    {
        return authManager;
    }

    public String getEncryptedPassword()
    {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword)
    {
        this.encryptedPassword = encryptedPassword;
    }

    public Profile getProfile()
    {
        if (StringUtils.isBlank(this.getCurrentProvider()) || !providers.containsKey(this.getCurrentProvider())) {
            return null;
        }
        try {
            return providers.get(this.getCurrentProvider()).getUserProfile();
        } catch (Exception e) {
            return null;
        }
    }

    public Profile getProfile(String provider)
    {
        if (!providers.containsKey(provider)) {
            return null;
        }
        try {
            return providers.get(provider).getUserProfile();
        } catch (Exception e) {
            return null;
        }
    }

    public AuthProvider getAuthProvider(String provider)
    {
        return providers.get(provider);
    }

    public void putAuthProvider(String provider, AuthProvider manager)
    {
        this.providers.put(provider, manager);
    }

    public String getCurrentProvider()
    {
        return currentProvider;
    }

    public void setCurrentProvider(String currentProvider)
    {
        this.currentProvider = currentProvider;
    }

}
