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

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWikiContext;

@Component("socialAuthDocument")
public class SocialAuthDocumentConfigurationSource extends AbstractDocumentConfigurationSource
{
    @Inject
    private Execution execution;

    @Inject
    private EntityReferenceValueProvider valueProvider;

    private static final String GLOBAL_CONFIGURATION_KEY = "xwiki.authentication.socialLogin.globalConfiguration";

    private static final String IGNORE_PREFIX = "socialauth.";

    private static final String XWIKI_SPACE = "XWiki";

    private static final String SOCIAL_LOGIN_CONFIGURATION_CLASS = "SocialLoginConfigurationClass";

    private static final String SOCIAL_LOGIN_CONFIGURATION_DOCUMENT = "SocialLoginConfiguration";

    @Override
    protected DocumentReference getClassReference()
    {
        if (isGlobalConfiguration()) {
                return new DocumentReference(getMainWikiName(), XWIKI_SPACE, SOCIAL_LOGIN_CONFIGURATION_CLASS);
        } else {
                return new DocumentReference(SOCIAL_LOGIN_CONFIGURATION_CLASS, new SpaceReference(XWIKI_SPACE,
                    getCurrentWikiReference()));
        }
    }

    @Override
    protected DocumentReference getDocumentReference()
    {
        if (isGlobalConfiguration()) {
            return new DocumentReference(getMainWikiName(), XWIKI_SPACE, SOCIAL_LOGIN_CONFIGURATION_DOCUMENT);
        } else {
            return new DocumentReference(SOCIAL_LOGIN_CONFIGURATION_DOCUMENT, new SpaceReference(XWIKI_SPACE,
                getCurrentWikiReference()));
        }
    }

    @Override
    public <T> T getProperty(String key)
    {
        // Remove the "socialauth." prefix when looking up key properties
        return super.getProperty(StringUtils.substringAfter(key, IGNORE_PREFIX));
    }
    
    @Override
    public boolean containsKey(String key)
    {
        // Remove the "socialauth." prefix when looking up key properties
        return super.containsKey(StringUtils.substringAfter(key, IGNORE_PREFIX));
    }

    private boolean isGlobalConfiguration() {
        String globalConfig = getContext().getWiki().Param(GLOBAL_CONFIGURATION_KEY);
        return "1".equals(globalConfig);
    }

    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    private String getMainWikiName() {
        String wikiName = valueProvider.getDefaultValue(EntityType.WIKI);
        return wikiName;
    }
}
