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
        return this.getPropertyInternal(StringUtils.substringAfter(key, IGNORE_PREFIX));
    }
    
    @Override
    public boolean containsKey(String key)
    {
        // Remove the "socialauth." prefix when looking up key properties
        return this.containsKeyInternal(StringUtils.substringAfter(key, IGNORE_PREFIX));
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
    
    // -------------
    
    // Override methods from AbstractDocumentConfigurationSource which are buggy in 3.5.1, because 
    
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
            result = getDocumentAccessBridge().getProperty(documentReference, classReference, key);
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
}
