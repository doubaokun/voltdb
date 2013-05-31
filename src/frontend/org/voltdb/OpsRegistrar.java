/* This file is part of VoltDB.
 * Copyright (C) 2008-2013 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.voltdb;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.voltcore.messaging.HostMessenger;

public class OpsRegistrar {
    private Map<OpsSelector, OpsAgent> m_agents;

    public OpsRegistrar() {
        m_agents = new HashMap<OpsSelector, OpsAgent>();
        for (OpsSelector selector : OpsSelector.values()) {
            try {
                Constructor<?> constructor = selector.getAgentClass()
                        .getConstructor();
                OpsAgent newAgent = (OpsAgent) constructor.newInstance();
                m_agents.put(selector, newAgent);
            } catch (Exception e) {
                VoltDB.crashLocalVoltDB(
                        "Unable to instantiate OpsAgent for selector: "
                                + selector.name(), true, e);
            }
        }
    }

    public void registerMailboxes(HostMessenger messenger) {
        for (Entry<OpsSelector, OpsAgent> entry : m_agents.entrySet()) {
            entry.getValue().registerMailbox(messenger,
                    entry.getKey().getHSId(messenger.getHostId()));
        }
    }

    public OpsAgent getAgent(OpsSelector selector) {
        OpsAgent agent = m_agents.get(selector);
        assert (agent != null);
        return agent;
    }

    public void shutdown() {
        for (Entry<OpsSelector, OpsAgent> entry : m_agents.entrySet()) {
            try {
                entry.getValue().shutdown();
            }
            catch (InterruptedException e) {}
        }
        m_agents.clear();
    }
}