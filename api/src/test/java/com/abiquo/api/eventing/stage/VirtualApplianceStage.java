/**
 * Abiquo community edition
 * cloud management application for hybrid clouds
 * Copyright (C) 2008-2010 - Abiquo Holdings S.L.
 *
 * This application is free software; you can redistribute it and/or
 * modify it under the terms of the GNU LESSER GENERAL PUBLIC
 * LICENSE as published by the Free Software Foundation under
 * version 3 of the License
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * LESSER GENERAL PUBLIC LICENSE v.3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package com.abiquo.api.eventing.stage;

import java.util.HashSet;
import java.util.Set;

import com.abiquo.server.core.cloud.State;

public class VirtualApplianceStage
{
    protected State state;

    protected State substate;

    protected State expectedState;

    protected State expectedSubstate;

    protected Stage parent;

    protected Set<VirtualMachineStage> machineStages;

    public VirtualApplianceStage(Stage stage)
    {
        this.parent = stage;
        this.machineStages = new HashSet<VirtualMachineStage>();
    }

    public VirtualApplianceStage in(State state, State substate)
    {
        this.state = state;
        this.substate = substate;

        return this;
    }

    public VirtualApplianceStage expecting(State state, State substate)
    {
        this.expectedState = state;
        this.expectedSubstate = substate;

        return this;
    }

    public VirtualMachineStage withVirtualMachine()
    {
        VirtualMachineStage stage = new VirtualMachineStage(this);
        machineStages.add(stage);

        return stage;
    }

    public Stage create()
    {
        return parent;
    }

    public Set<VirtualMachineStage> getMachineStages()
    {
        return machineStages;
    }

    public State getState()
    {
        return state;
    }

    public State getSubstate()
    {
        return substate;
    }

    public State getExpectedState()
    {
        return expectedState;
    }

    public State getExpectedSubstate()
    {
        return expectedSubstate;
    }
}
