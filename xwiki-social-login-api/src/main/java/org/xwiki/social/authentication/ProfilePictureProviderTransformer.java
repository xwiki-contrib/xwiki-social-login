package org.xwiki.social.authentication;

import org.xwiki.component.annotation.Role;

@Role
public interface ProfilePictureProviderTransformer
{
    String transform(String url);
}
