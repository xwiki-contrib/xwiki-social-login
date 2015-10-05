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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.social.authentication.SocialAuthConfiguration;

@Component
public class DefaultSocialAuthConfiguration implements SocialAuthConfiguration
{

    /**
     * Prefix for configuration keys for the social login module.
     */
    private static final String PREFIX = "socialauth.";

    /**
     * Defines from where to read the rendering configuration data.
     */
    @Inject
    @Named("socialAuth")
    private ConfigurationSource configuration;

    @Override
    public List<String> getAvailableProviders()
    {
        return Arrays.asList(this.configuration.getProperty(PREFIX + "availableProviders", "").split("[,\n]"));
    }

    @Override
    public boolean getLoginButtonsEnabled()
    {
        Object value = this.configuration.getProperty(PREFIX + "loginButtonsEnabled", "1");
        try {
            // Document fields of type boolean actually store/return integers :/
            return (Integer) value > 0;
        } catch (ClassCastException e) {
            try {
                return (Boolean) value;
            } catch (ClassCastException e2) {
                // Typically when the XClass does not exist yet.
                return false;
            }
        }
    }

    @Override
    public boolean isAutomaticUserCreation()
    {
        Object value = this.configuration.getProperty(PREFIX + "automaticUserCreation", "1");
        try {
            // Document fields of type boolean actually store/return integers :/
            return (Integer) value > 0;
        } catch (ClassCastException e) {
            try {
                return (Boolean) value;
            } catch (ClassCastException e2) {
                // Typically when the XClass does not exist yet.
                return false;
            }
        }
    }

    @Override
    public String getDomainRestriction()
    {
        return this.configuration.getProperty(PREFIX + "domainRestriction", "");
    }
}
