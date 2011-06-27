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

package com.abiquo.api.eventing;

import static com.abiquo.api.eventing.stage.Stage.createStage;
import static com.abiquo.server.core.cloud.State.BUNDLING;
import static com.abiquo.server.core.cloud.State.IN_PROGRESS;
import static com.abiquo.server.core.cloud.State.RUNNING;

import javax.persistence.EntityManager;

import org.testng.annotations.Test;

import com.abiquo.server.core.cloud.State;
import com.abiquo.vsm.events.VMEventType;

/**
 * Test cases for virtual appliances updating.
 * 
 * @author eruiz@abiquo.com
 */
public class VirtualApplianceUpdateStateTest extends EventingProcessorTestBase
{
    @Test
    public void test_UpdateStateToRunningRunning()
    {
        runTestStage(createStage().withAppliance().in(IN_PROGRESS, IN_PROGRESS)
            .expecting(RUNNING, RUNNING).withVirtualMachine().in(IN_PROGRESS).expecting(RUNNING)
            .onEvent(VMEventType.POWER_ON).and().withVirtualMachine().in(IN_PROGRESS)
            .expecting(State.PAUSED).onEvent(VMEventType.PAUSED).and().withVirtualMachine()
            .in(IN_PROGRESS).expecting(State.POWERED_OFF).onEvent(VMEventType.POWER_OFF).create());
    }

    @Test
    public void test_NotUpdateStateOnBundling()
    {
        runTestStage(createStage().withAppliance().in(IN_PROGRESS, BUNDLING)
            .expecting(IN_PROGRESS, BUNDLING).withVirtualMachine().in(IN_PROGRESS)
            .expecting(RUNNING).onEvent(VMEventType.POWER_ON).and().withVirtualMachine()
            .in(IN_PROGRESS).expecting(State.PAUSED).onEvent(VMEventType.PAUSED).and()
            .withVirtualMachine().in(IN_PROGRESS).expecting(State.POWERED_OFF)
            .onEvent(VMEventType.POWER_OFF).create());
    }

    @Override
    protected EventingProcessor getEventingProcessor(EntityManager em)
    {
        return new EventingProcessor(em);
    }
}
