package org.xwiki.social.authentication.internal;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.brickred.socialauth.Profile;
import org.brickred.socialauth.util.SocialAuthUtil;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.social.authentication.SocialAuthConfiguration;
import org.xwiki.social.authentication.SocialAuthException;
import org.xwiki.social.authentication.SocialAuthSession;
import org.xwiki.social.authentication.SocialAuthenticationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

@Component("socialAuth")
public class SocialAuthScriptService implements ScriptService
{

    @Inject
    private SocialAuthenticationManager socialAuthManager;

    @Inject
    private SocialAuthConfiguration socialAuthConfiguration;

    @Inject
    private Execution execution;

    public boolean associateAccount(String provider)
    {
        if (socialAuthManager.hasProvider(getContextUser(), provider)) {
            // Nothing to do
            return false;
        }

        try {
            this.socialAuthManager.associateAccount(provider);
            return true;
        } catch (SocialAuthException e) {
            return false;
        }
    }

    public boolean getLoginButtonsEnabled()
    {
        return this.socialAuthConfiguration.getLoginButtonsEnabled();
    }

    public List<String> getAvailableProviders()
    {
        return this.socialAuthConfiguration.getAvailableProviders();
    }

    public Profile getSessionProfile()
    {
        if (this.socialAuthManager.getSession() == null) {
            return null;
        }
        return this.socialAuthManager.getSession().getProfile();
    }

    public boolean registerUser()
    {
        try {
            SocialAuthSession session = socialAuthManager.getSession();
            Map<String, String> parameters = SocialAuthUtil.getRequestParametersMap(getContext().getRequest());
            session.getProfile();
            this.socialAuthManager.createUser(parameters);
            return true;
        } catch (SocialAuthException e) {
            getContext().put("message", e.getMessage());
            return false;
        } catch (XWikiException e) {
            getContext().put("message", e.getMessage());
            return false;
        }
    }

    public boolean registerUser(String username)
    {
        try {
            SocialAuthSession session = socialAuthManager.getSession();
            Map<String, String> parameters = SocialAuthUtil.getRequestParametersMap(getContext().getRequest());
            session.getProfile();
            this.socialAuthManager.createUser(username, parameters);
            return true;
        } catch (SocialAuthException e) {
            getContext().put("message", e.getMessage());
            return false;
        } catch (XWikiException e) {
            getContext().put("message", e.getMessage());
            return false;
        }
    }

    public boolean hasProvider(String provider)
    {
        return this.socialAuthManager.hasProvider(getContextUser(), provider);
    }

    public boolean isConnected(String provider)
    {
        return this.socialAuthManager.isConnected(provider);
    }

    public String getToken(String provider)
    {
        SocialAuthSession session = this.socialAuthManager.getSession();
        if (session == null || session.getAuthProvider(provider) == null) {
            return null;
        }
        return session.getAuthProvider(provider).getAccessGrant().getKey();
    }

    // /////////////////////////////////////////////////////////////////////////

    private DocumentReference getContextUser()
    {
        return this.getContext().getUserReference();
    }

    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
}
