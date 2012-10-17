package org.xwiki.social.authentication.internal;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.CompositeConfigurationSource;

@Component("socialAuth")
public class SocialAuthConfigurationSource extends CompositeConfigurationSource implements Initializable
{
    @Inject
    @Named("socialAuthDocument")
    private ConfigurationSource socialAuthDocumentSource;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    public void initialize() throws InitializationException
    {
        // First source is searched first when a property value is requested.
        this.addConfigurationSource(socialAuthDocumentSource);
        this.addConfigurationSource(xwikiPropertiesSource);
    }
}
