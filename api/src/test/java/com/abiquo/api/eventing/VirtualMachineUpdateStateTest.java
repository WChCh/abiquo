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
import static com.abiquo.server.core.cloud.State.HA_IN_PROGRESS;
import static com.abiquo.server.core.cloud.State.IN_PROGRESS;

import javax.persistence.EntityManager;

import org.testng.annotations.Test;

import com.abiquo.vsm.events.VMEventType;

/**
 * Test cases for virtual machines updating.
 * 
 * @author eruiz@abiquo.com
 */
public class VirtualMachineUpdateStateTest extends EventingProcessorTestBase
{
    private static final String INVALID_EVENT = "INVALID_EVENT";

    @Test
    public void test_IgnoreUnknownEventTest()
    {
        runTestStage(createStage().withAppliance().in(IN_PROGRESS, IN_PROGRESS)
            .expecting(IN_PROGRESS, IN_PROGRESS).withVirtualMachine().in(IN_PROGRESS)
            .expecting(IN_PROGRESS).onEvent(INVALID_EVENT).create());
    }

    @Test
    public void test_IgnoreEventWithHighAvailabilityInProgress()
    {
        runTestStage(createStage().withAppliance().in(IN_PROGRESS, IN_PROGRESS)
            .expecting(IN_PROGRESS, IN_PROGRESS).withVirtualMachine().in(HA_IN_PROGRESS)
            .expecting(HA_IN_PROGRESS).onEvent(VMEventType.POWER_ON).create());
    }

    @Test
    public void test_IgnoreMovedEvents()
    {
        runTestStage(createStage().withAppliance().in(IN_PROGRESS, IN_PROGRESS)
            .expecting(IN_PROGRESS, IN_PROGRESS).withVirtualMachine().in(IN_PROGRESS)
            .expecting(IN_PROGRESS).onEvent(VMEventType.MOVED).create());
    }

    @Test
    public void test_IgnoreDestroyedEvents()
    {
        runTestStage(createStage().withAppliance().in(IN_PROGRESS, IN_PROGRESS)
            .expecting(IN_PROGRESS, IN_PROGRESS).withVirtualMachine().in(IN_PROGRESS)
            .expecting(IN_PROGRESS).onEvent(VMEventType.DESTROYED).create());
    }

    @Override
    protected EventingProcessor getEventingProcessor(EntityManager em)
    {
        return new EventingProcessor(em);
    }
}
