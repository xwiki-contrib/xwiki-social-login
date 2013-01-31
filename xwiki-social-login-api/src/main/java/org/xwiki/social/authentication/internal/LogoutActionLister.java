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
package org.xwiki.social.authentication.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.bridge.event.ActionExecutedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.social.authentication.SocialAuthenticationManager;

/**
 * Called when logout action is executed to cleanup the session.
 * @version $Id$
 */
@Component
@Named("org.xwiki.social.authentication.internal.LogoutActionLister")
public class LogoutActionLister implements EventListener
{
    /**
     * Listened events.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new ActionExecutedEvent("logout"));

    @Inject
    private SocialAuthenticationManager socialAuthManager;

    // EventListener

    @Override
    public String getName()
    {
        return "org.xwiki.social.authentication.internal.LogoutActionLister";
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object arg1, Object arg2)
    {
        this.socialAuthManager.removeSession();
    }
}
