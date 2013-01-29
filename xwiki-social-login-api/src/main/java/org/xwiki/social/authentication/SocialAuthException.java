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

public class SocialAuthException extends Exception
{

    /**
     * Generated serial UID. Change when the serialization of this class changes. 
     */
    private static final long serialVersionUID = -6040421105822572551L;

    public SocialAuthException()
    {
        super();
    }

    public SocialAuthException(String message)
    {
        super(message);
    }

    public SocialAuthException(String message, Throwable t)
    {
        super(message, t);
    }

}
