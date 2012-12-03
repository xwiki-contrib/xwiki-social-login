package org.xwiki.social.authentication;

import org.xwiki.component.annotation.ComponentRole;

@ComponentRole
public interface ProfilePictureProviderTransformer
{
    String transform(String url);
}
