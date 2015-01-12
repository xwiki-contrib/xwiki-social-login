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

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;

@Component("socialAuthDocument")
public class SocialAuthDocumentConfigurationSource implements ConfigurationSource
{
    @Inject
    private Execution execution;

    @Inject
    private EntityReferenceValueProvider valueProvider;

    /**
     * @see #getDocumentAccessBridge()
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /** @see #getCurrentWikiReference() */
    @Inject
    private ModelContext modelContext;

    /** @see #getCurrentWikiReference() */
    @Inject
    private ModelConfiguration modelConfig;
    
    private static final String GLOBAL_CONFIGURATION_KEY = "xwiki.authentication.socialLogin.globalConfiguration";

    private static final String IGNORE_PREFIX = "socialauth.";

    private static final String XWIKI_SPACE = "XWiki";

    private static final String SOCIAL_LOGIN_CONFIGURATION_CLASS = "SocialLoginConfigurationClass";

    private static final String SOCIAL_LOGIN_CONFIGURATION_DOCUMENT = "SocialLoginConfiguration";

    /**
     * @return the XWiki Class reference of the XWiki Object containing the configuration properties
     */
    protected DocumentReference getClassReference()
    {
        if (isGlobalConfiguration()) {
            return new DocumentReference(getMainWikiName(), XWIKI_SPACE, SOCIAL_LOGIN_CONFIGURATION_CLASS);
        } else {
            return new DocumentReference(SOCIAL_LOGIN_CONFIGURATION_CLASS, new SpaceReference(XWIKI_SPACE,
                getCurrentWikiReference()));
        }
    }

    /**
     * @return the document reference of the document containing an XWiki Object with configuration data or null if
     *         there no such document in which case this configuration source will be skipped
     */
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
        return this.getPropertyInternal(StringUtils.substringAfter(key, IGNORE_PREFIX));
    }

    @Override
    public boolean containsKey(String key)
    {
        // Remove the "socialauth." prefix when looking up key properties
        return this.containsKeyInternal(StringUtils.substringAfter(key, IGNORE_PREFIX));
    }

    private boolean isGlobalConfiguration()
    {
        String globalConfig = getContext().getWiki().Param(GLOBAL_CONFIGURATION_KEY);
        return "1".equals(globalConfig);
    }

    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    private String getMainWikiName()
    {
        String wikiName = valueProvider.getDefaultValue(EntityType.WIKI);
        return wikiName;
    }

    // -------------
    // Re-implement AbstractDocumentConfigurationSource since it's internal and have changed several times

    public <T> T getPropertyInternal(String key)
    {
        return (T) getPropertyObject(key);
    }

    public boolean containsKeyInternal(String key)
    {
        return getPropertyObject(key) != null;
    }

    private Object getPropertyObject(String key)
    {
        Object result;

        DocumentReference documentReference = getFailsafeDocumentReference();
        DocumentReference classReference = getFailsafeClassReference();
        if (documentReference != null && classReference != null) {
            result = this.documentAccessBridge.getProperty(documentReference, classReference, key);
        } else {
            result = null;
        }

        return result;
    }

    @Override
    public boolean isEmpty()
    {
        return getKeys().isEmpty();
    }

    private DocumentReference getFailsafeDocumentReference()
    {
        DocumentReference documentReference;

        try {
            documentReference = getDocumentReference();
        } catch (Exception e) {
            // We verify that no error has happened and if one happened then we skip this configuration source. This
            // ensures the system will continue to work even if this source has a problem.
            documentReference = null;
        }

        return documentReference;
    }

    private DocumentReference getFailsafeClassReference()
    {
        DocumentReference classReference;

        try {
            classReference = getClassReference();
        } catch (Exception e) {
            // We verify that no error has happened and if one happened then we skip this configuration source. This
            // ensures the system will continue to work even if this source has a problem.
            classReference = null;
        }

        return classReference;
    }

    /**
     * @return the reference pointing to the current wiki
     */
    protected WikiReference getCurrentWikiReference()
    {
        if (this.modelContext.getCurrentEntityReference() != null) {
            return (WikiReference) this.modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI);
        }

        return new WikiReference(this.modelConfig.getDefaultReferenceValue(EntityType.WIKI));
    }

    @Override
    public List<String> getKeys()
    {
        return null;
    }

    @Override
    public <T> T getProperty(String key, T defaultValue)
    {
        T result = getProperty(key);

        if (result == null) {
            result = defaultValue;
        }

        return result;
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass)
    {
        T result = getProperty(key);

        // Make sure we don't return null values for List and Properties (they must return empty elements
        // when using the typed API).
        if (result == null) {
            result = getDefault(valueClass);
        }

        return result;
    }

    /**
     * @param valueClass the class of the property
     * @param <T> the type of the property
     * @return the default value of a property for the provided class
     */
    private <T> T getDefault(Class<T> valueClass)
    {
        T result = null;

        if (valueClass != null) {
            if (List.class.getName().equals(valueClass.getName())) {
                result = (T) Collections.emptyList();
            } else if (Properties.class.getName().equals(valueClass.getName())) {
                result = (T) new Properties();
            }
        }

        return result;
    }
}
