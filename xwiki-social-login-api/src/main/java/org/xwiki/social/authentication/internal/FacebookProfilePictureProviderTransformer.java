package org.xwiki.social.authentication.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.social.authentication.ProfilePictureProviderTransformer;

@Component("facebook")
public class FacebookProfilePictureProviderTransformer implements ProfilePictureProviderTransformer
{

    public String transform(String url)
    {
        return url + "?type=large";
    }

}
