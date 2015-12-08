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
package org.xwiki.social.authentication;

import org.xwiki.model.reference.DocumentReference;

public interface SocialAuthConstants
{
    static final String CALLBACK_PARAMETER = "sl_callback";

    static final String PROVIDER_PARAMETER = "sl_provider";
    
    static final String SOCIAL_AUTH_SESSION_ATTRIBUTE = "org.xwiki.social.authentication.SocialAuthSession";

    static final DocumentReference SOCIAL_LOGIN_PROFILE_CLASS = new DocumentReference("xwiki", "XWiki", "SocialLoginProfileClass");

    static final DocumentReference XWIKI_USER_CLASS_REF = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
}
