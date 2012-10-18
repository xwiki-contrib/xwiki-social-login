Installation
============

Besides importing the application, you'll need to :
* setup an oauth_consumer.properties file, as specified [here](http://code.google.com/p/socialauth/wiki/SampleProperties)
* set the authclass parameter (xwiki.authentication.authclass) to "org.xwiki.social.authentication.internal.SocialAuthServiceImpl" in xwiki.cfg
* if you want a global setup on a XEM (only global users, set xwiki.authentication.socialLogin.globalConfiguration to "1" also in xwiki.cfg
* copy the login.vm file in src/main/webapp to your wiki skin
* add XWiki.SocialLoginTranslations to your wiki i18n documents bundle
* visit your wiki administration "social auth" section to configure more settings
