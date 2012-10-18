package org.xwiki.social.authentication.internal;

import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang3.StringUtils;
import org.brickred.socialauth.AuthProvider;
import org.brickred.socialauth.Profile;
import org.brickred.socialauth.SocialAuthConfig;
import org.brickred.socialauth.SocialAuthManager;
import org.brickred.socialauth.util.SocialAuthUtil;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.crypto.passwd.PasswordCryptoService;
import org.xwiki.environment.Environment;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.social.authentication.ProfilePictureProviderTransformer;
import org.xwiki.social.authentication.SocialAuthConfiguration;
import org.xwiki.social.authentication.SocialAuthConstants;
import org.xwiki.social.authentication.SocialAuthException;
import org.xwiki.social.authentication.SocialAuthSession;
import org.xwiki.social.authentication.SocialAuthenticationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component
public class DefaultSocialAuthManager implements SocialAuthenticationManager, SocialAuthConstants
{
    private static final String DEFAULT_PROFILE_PICTURE_FILENAME = "profile.jpg";

    private static final String GLOBAL_CONFIGURATION_KEY = "xwiki.authentication.socialLogin.globalConfiguration";

    private static final String EXTRA_REGISTRATION_STEP_DOCUMENT = "XWiki.SocialLoginRegister";

    @Inject
    private Logger logger;

    @Inject
    private Environment environment;

    @Inject
    private Execution execution;

    @Inject
    private QueryManager queryManager;

    @Inject
    private SocialAuthConfiguration configuration;

    @Inject
    private PasswordCryptoService passwordCryptoService;

    @Inject
    private Map<String, ProfilePictureProviderTransformer> profilePictureTransformers;

    @Inject
    private EntityReferenceValueProvider valueProvider;

    private SocialAuthConfig config;

    @Override
    public void associateAccount(String providerId) throws SocialAuthException
    {
        XWikiContext context = getContext();
        HttpServletRequest request = context.getRequest();
        AuthProvider provider;
        try {
            if (StringUtils.isBlank(request.getParameter(CALLBACK_PARAMETER))) {
                String url =
                    request.getRequestURL() + "?" + request.getQueryString() + "&" + CALLBACK_PARAMETER + "=1&"
                        + PROVIDER_PARAMETER + "=" + providerId;

                this.requestConnection(providerId, url);

            } else {
                SocialAuthSession session =
                    (SocialAuthSession) request.getSession().getAttribute(SOCIAL_AUTH_SESSION_ATTRIBUTE);

                provider = session.getAuthManager().connect(SocialAuthUtil.getRequestParametersMap(request));
                session.putAuthProvider(providerId, provider);
                Profile profile = provider.getUserProfile();

                if (getUser(providerId, profile.getValidatedId()) != null) {
                    throw new SocialAuthException(
                        "Refusing to associate account as it is already associated with a user on this wiki.");
                }

                this.addSocialProfileToUser(profile, getContext().getUserReference());
            }
        } catch (Exception e) {
            throw new SocialAuthException("Failed to associate account", e);
        }
    }

    @Override
    public DocumentReference connect(Map<String, String> requestParameters) throws SocialAuthException
    {
        AuthProvider provider;
        SocialAuthSession session = getSession();
        try {
            SocialAuthManager manager = session.getAuthManager();
            provider = manager.connect(requestParameters);
            Profile profile = provider.getUserProfile();

            // check eventual domain restriction
            String domainRestriction = configuration.getDomainRestriction();
            if (!StringUtils.isBlank(domainRestriction)) {
                if (StringUtils.isBlank(profile.getEmail()) || !profile.getEmail().endsWith(domainRestriction)) {
                    // user email does not match the proper domain, we need to refuse it
                    XWikiContext context = getContext();
                    context.put("message", "xwiki.socialLogin.unauthorizedDomainError");
                    throw new SocialAuthException(
                        "Failed to validate connection because email is not matching the authorized domain");
                }
            }

            boolean isGlobalConfiguration = isGlobalConfiguration();
            XWikiContext context = getContext();
            String currentDatabase = context.getDatabase();
            if (isGlobalConfiguration) {
                // we need to make sure this happens in the main wiki if the configuration says so
                context.setDatabase(getMainWikiName());
            }

            try {
                DocumentReference user = getUser(profile.getProviderId(), profile.getValidatedId());

                // FIXME use a random in a singleton instead, as somebody could use the persistent cookie to forge the
                // encrypted password
                String key = getEncryptionKey();
                session.putAuthProvider(profile.getProviderId(), provider);
                session.setCurrentProvider(profile.getProviderId());

                if (user == null) {
                    if (configuration.isAutomaticUserCreation()) {
                        user = this.createUser(profile, this.computeUsername(profile));
                    } else {
                        getResponse().sendRedirect(
                            context.getWiki().getURL(EXTRA_REGISTRATION_STEP_DOCUMENT, "view", context));
                        return null;
                    }
                }

                XWikiDocument userDocument = getContext().getWiki().getDocument(user, getContext());
                BaseObject object =
                    userDocument.getObject(SOCIAL_LOGIN_PROFILE_CLASS, "provider", profile.getProviderId());
                String password = object.getStringValue("password");
                this.setPassword(password);

                return user;
            } finally {
                if (isGlobalConfiguration) {
                    context.setDatabase(currentDatabase);
                }
            }

        } catch (Exception e) {
            throw new SocialAuthException("Failed to validate connection", e);
        }

    }

    @Override
    public DocumentReference createUser(Map<String, String> extraProperties) throws XWikiException, SocialAuthException
    {
        SocialAuthSession session = getSession();
        if (session == null || session.getProfile() == null) {
            throw new SocialAuthException("Illegal attempt at creating a user that is not associated");
        }
        return this.createUser(this.computeUsername(session.getProfile()), extraProperties);
    }

    @Override
    public DocumentReference createUser(String username, Map<String, String> extraProperties) throws XWikiException,
        SocialAuthException
    {
        SocialAuthSession session = getSession();
        if (session == null || session.getProfile() == null) {
            throw new SocialAuthException("Illegal attempt at creating a user that is not associated");
        }
        return this.createUser(session.getProfile(), username, extraProperties);
    }

    @Override
    public SocialAuthSession getSession()
    {
        HttpSession httpSession = getRequest().getSession();
        SocialAuthSession session = (SocialAuthSession) httpSession.getAttribute(SOCIAL_AUTH_SESSION_ATTRIBUTE);
        return session;
    }

    @Override
    public DocumentReference getUser(String provider, String id)
    {
        // we need to make sure this happens in the main wiki if the configuration says so
        // this is already done in connect() but getUser can also be called from the Authenticator
        boolean isGlobalConfiguration = isGlobalConfiguration();
        XWikiContext context = getContext();
        String currentDatabase = context.getDatabase();
        if (isGlobalConfiguration) {
            context.setDatabase(getMainWikiName());
        }

        try {
            String queryStatement =
                "from doc.object(XWiki.XWikiUsers) as user, doc.object(XWiki.SocialLoginProfileClass)"
                    + " as profile where profile.provider = :provider and profile.validatedId = :validated";

            Query query = this.queryManager.createQuery(queryStatement, Query.XWQL);
            query.bindValue("provider", provider);
            query.bindValue("validated", id);

            List<String> results = query.execute();

            for (String reference : results) {
                return getContext().getWiki().getDocument(reference, getContext()).getDocumentReference();
            }
            return null;
        } catch (QueryException e) {
            this.logger.error("Failed to query for user with provider [{}] and id [{}]", provider, id);
            return null;
        } catch (XWikiException e) {
            this.logger.error("Failed to query for user with provider [{}] and id [{}]", provider, id);
            return null;
        } finally {
            if (isGlobalConfiguration) {
                context.setDatabase(currentDatabase);
            }
        }
    }

    @Override
    public boolean hasProvider(DocumentReference user, String provider)
    {
        XWikiDocument userDocument;
        try {
            userDocument = getContext().getWiki().getDocument(user, getContext());
            BaseObject object = userDocument.getObject(SOCIAL_LOGIN_PROFILE_CLASS, "provider", provider);
            return object != null;
        } catch (XWikiException e) {
            this.logger
                .error(MessageFormat.format("Failed to determine if user [{0}] has associated provider [{1}]", user,
                    provider), e);
            return false;
        }

    }

    @Override
    public boolean isConnected()
    {
        return isConnected(getSession());
    }

    @Override
    public boolean isConnected(String provider)
    {
        return isConnected(getSession(), provider);
    }

    @Override
    public void requestConnection(String provider, String returnUrl) throws SocialAuthException
    {
        HttpSession httpSession = getRequest().getSession();
        HttpServletResponse response = getResponse();

        try {
            SocialAuthManager manager = new SocialAuthManager();
            manager.setSocialAuthConfig(this.getSocialAuthConfig());

            // Save the manager in session : we will need this one on the way back from OAuth to validate the response
            SocialAuthSession session = new SocialAuthSession(manager);
            httpSession.setAttribute(SOCIAL_AUTH_SESSION_ATTRIBUTE, session);

            String url = manager.getAuthenticationUrl(provider, returnUrl);

            logger.debug("Redirecting to OAuth endpoint URL : " + url);

            try {
                response.sendRedirect(url);
            } catch (java.lang.IllegalStateException e) {
                // Avoid "response already committed" exception in the logs...
                // FIXME need to find a way to perform a redirection cleanly from an authenticator.
            }

        } catch (Exception e) {
            throw new SocialAuthException("Error when requesting connection", e);
        }
    }

    @Override
    public boolean userExists(String provider, String id)
    {
        return getUser(provider, id) != null;
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    
    private void addSocialProfileToUser(Profile profile, DocumentReference user) throws SocialAuthException
    {
        boolean isGlobalConfiguration = isGlobalConfiguration();
        XWikiContext context = getContext();
        String currentDatabase = context.getDatabase();
        if (isGlobalConfiguration) {
            // we need to make sure this happens in the main wiki if the configuration says so
            // this is already done in connect() but getUser can also be called from the Authenticator
            context.setDatabase(getMainWikiName());
        }

        try {
            XWikiDocument userDoc = context.getWiki().getDocument(user, context);

            String generatedPassword = getContext().getWiki().generateRandomString(16);

            BaseObject socialProfile = userDoc.getObject(SOCIAL_LOGIN_PROFILE_CLASS, true, context);
            BaseObject userObject = userDoc.getObject("XWiki.XWikiUsers", false, context);

            if (userObject == null) {
                throw new SocialAuthException("Cannot associate a social profile to a non-user page");
            }

            if (StringUtils.isBlank(userObject.getStringValue("first_name"))) {
                userObject.set("first_name", profile.getFirstName(), context);
            }
            if (StringUtils.isBlank(userObject.getStringValue("last_name"))) {
                userObject.set("last_name", profile.getLastName(), context);
            }
            if (StringUtils.isBlank(userObject.getStringValue("email"))) {
                userObject.set("email", profile.getEmail(), context);
            }
            
            if (!StringUtils.isBlank(profile.getProfileImageURL())
                && StringUtils.isBlank(userObject.getStringValue("avatar"))) {

                String profilePictureURL = profile.getProfileImageURL();
                if (this.profilePictureTransformers.containsKey(profile.getProviderId())) {
                    // Transform profile picture URL if necessary for this provider
                    profilePictureURL =
                        this.profilePictureTransformers.get(profile.getProviderId()).transform(profilePictureURL);
                }

                GetMethod get = new GetMethod(profilePictureURL);
                try {
                    XWikiAttachment attachment = new XWikiAttachment(userDoc, DEFAULT_PROFILE_PICTURE_FILENAME);
                    userDoc.getAttachmentList().add(attachment);

                    HttpClient httpClient = new HttpClient();
                    httpClient.getParams().setBooleanParameter("http.connection.stalecheck", true);

                    int httpStatus = httpClient.executeMethod(get);
                    if (httpStatus == HttpStatus.SC_OK) {
                        attachment.setContent(get.getResponseBodyAsStream());
                        attachment.setAuthor(userDoc.getAuthor());
                        attachment.setDoc(userDoc);
                        userObject.set("avatar", DEFAULT_PROFILE_PICTURE_FILENAME, context);
                    } else {
                        this.logger.debug("Failed to load image: status is " + httpStatus);
                    }

                } catch (Exception e) {
                    this.logger.warn("Error attaching social profile picture to profile", e);
                    // Nevermind, ain't gonna have a profile picture.
                } finally {
                    get.releaseConnection();
                }
            }

            socialProfile.set("provider", profile.getProviderId(), context);
            socialProfile.set("fullName", profile.getFullName(), context);
            socialProfile.set("firstName", profile.getFirstName(), context);
            socialProfile.set("lastName", profile.getLastName(), context);
            socialProfile.set("displayName", profile.getDisplayName(), context);
            socialProfile.set("email", profile.getEmail(), context);
            socialProfile.set("profileImageURL", profile.getProfileImageURL(), context);
            socialProfile.set("gender", profile.getGender(), context);
            if (profile.getDob() != null) {
                socialProfile.set("dob", profile.getDob().toString(), context);
            }
            socialProfile.set("validatedId", profile.getValidatedId(), context);
            socialProfile.set("country", profile.getCountry(), context);
            socialProfile.set("location", profile.getLocation(), context);
            socialProfile.set("password", generatedPassword, context);

            this.setPassword(generatedPassword);

            context.getWiki().saveDocument(userDoc,
                context.getMessageTool().get("xwiki.socialLogin.updatedSocialProfile"), true, context);
        } catch (XWikiException e) {
            this.logger.error("Failed to merge or create user", e);
        } finally {
            if (isGlobalConfiguration) {
                context.setDatabase(currentDatabase);
            }
        }
    }
    
    private String computeUsername(Profile profile)
    {
        // TODO let the format be defined in configuration

        String username = profile.getDisplayName();
        if (StringUtils.isBlank(username)) {
            username = profile.getFirstName() + profile.getLastName();
        }
        if (StringUtils.isBlank(username)) {
            username = profile.getProviderId() + "-" + profile.getValidatedId();
        }

        return getContext().getWiki().getUniquePageName("XWiki", username, getContext());
    }

    private DocumentReference createUser(Profile profile, String username) throws XWikiException, SocialAuthException
    {
        return this.createUser(profile, username, Collections.<String, String> emptyMap());
    }

    private DocumentReference createUser(Profile profile, String username, Map<String, String> extraProperties)
        throws XWikiException, SocialAuthException
    {
        String userDocumentName = "XWiki." + username;

        if (isGlobalConfiguration()) {
            // we need to make sure we create the user globally if the configuration says so
            userDocumentName = getMainWikiName() + ":" + userDocumentName;
        }
        XWikiContext context = getContext();

        Map<String, String> properties = new HashMap<String, String>(extraProperties);
        properties.put("active", "1");
        properties.put("email", profile.getEmail());
        properties.put("first_name", profile.getFirstName());
        properties.put("last_name", profile.getLastName());
        // We don't put the same password as the one of the social profile
        properties.put("password", getContext().getWiki().generateRandomString(16));

        context.getWiki().createUser(username, properties, context);

        XWikiDocument userDoc = context.getWiki().getDocument(userDocumentName, context);

        this.addSocialProfileToUser(profile, userDoc.getDocumentReference());

        return userDoc.getDocumentReference();

    }
    
    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
    
    private String getEncryptionKey()
    {
        String key = getContext().getWiki().Param("xwiki.authentication.encryptionKey");
        return key;
    }

    private String getMainWikiName()
    {
        return valueProvider.getDefaultValue(EntityType.WIKI);
    }

    private HttpServletRequest getRequest()
    {
        return getContext().getRequest();
    }

    private HttpServletResponse getResponse()
    {
        return getContext().getResponse();
    }

    private boolean isGlobalConfiguration()
    {
        return "1".equals(getContext().getWiki().Param(GLOBAL_CONFIGURATION_KEY));
    }
    
    private SocialAuthConfig getSocialAuthConfig()
    {
        if (this.config == null) {
            try {
                Properties properties = new Properties();
                properties.load(this.environment.getResourceAsStream("/WEB-INF/oauth_consumer.properties"));

                config = SocialAuthConfig.getDefault();
                config.load(properties);

            } catch (Exception e) {
                logger.error("Failed to initialize Social Auth", e);
            }
        }
        return this.config;
    }

    private boolean isConnected(SocialAuthSession profile)
    {
        return profile != null && this.isConnected(profile, profile.getCurrentProvider());
    }

    private boolean isConnected(SocialAuthSession profile, String provider)
    {
        return profile != null && profile.getProfile(provider) != null;
    }

    private void setPassword(String password)
    {
        try {
            getSession().setEncryptedPassword(this.passwordCryptoService.encryptText(password, getEncryptionKey()));
        } catch (GeneralSecurityException e) {
            // Nothing
        }
    }
}
