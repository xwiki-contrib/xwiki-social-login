package org.xwiki.social.authentication;

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;

@Role
public interface SocialAuthenticationManager
{
    boolean userExists(String provider, String id);

    DocumentReference getUser(String provider, String id);

    boolean hasProvider(DocumentReference user, String provider);

    SocialAuthSession getSession();
    
    boolean isConnected();

    boolean isConnected(String provider);
    
    boolean isConnected(SocialAuthSession profile);

    boolean isConnected(SocialAuthSession profile, String provider);

    void requestConnection(String provider, String returnUrl) throws SocialAuthException;

    DocumentReference connect(Map<String, String> requestParameters) throws SocialAuthException;

    DocumentReference createUser(Map<String, String> extraProperties) throws XWikiException, SocialAuthException;

    DocumentReference createUser(String username, Map<String, String> extraProperties) throws XWikiException,
        SocialAuthException;

    void associateAccount(String provider) throws SocialAuthException;
}
