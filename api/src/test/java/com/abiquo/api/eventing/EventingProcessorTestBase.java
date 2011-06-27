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

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import com.abiquo.api.common.AbstractGeneratorTest;
import com.abiquo.api.eventing.stage.Stage;
import com.abiquo.api.eventing.stage.VirtualMachineStage;
import com.abiquo.commons.amqp.impl.vsm.domain.VirtualSystemEvent;
import com.abiquo.server.core.cloud.Hypervisor;
import com.abiquo.server.core.cloud.NodeVirtualImage;
import com.abiquo.server.core.cloud.VirtualAppliance;
import com.abiquo.server.core.cloud.VirtualMachine;
import com.abiquo.server.core.enterprise.Enterprise;
import com.softwarementors.bzngine.engines.jpa.EntityManagerHelper;

public abstract class EventingProcessorTestBase extends AbstractGeneratorTest
{
    protected abstract EventingProcessor getEventingProcessor(EntityManager em);

    protected void runTestStage(Stage stage)
    {
        // List of entities to persist
        List<Object> entitiesToPersist = new ArrayList<Object>();

        // Create enterprise
        Enterprise enterprise = enterpriseGenerator.createUniqueInstance();

        enterpriseGenerator.addAuxiliaryEntitiesToPersist(enterprise, entitiesToPersist);
        entitiesToPersist.add(enterprise);

        // Create hypervisor
        Hypervisor hypervisor = hypervisorGenerator.createUniqueInstance();
        hypervisor.getMachine().setEnterprise(enterprise);
        hypervisor.getMachine().setHypervisor(hypervisor);

        hypervisorGenerator.addAuxiliaryEntitiesToPersist(hypervisor, entitiesToPersist);
        entitiesToPersist.add(hypervisor);

        // Create virtual appliance
        VirtualAppliance vapp = virtualApplianceGenerator.createUniqueInstance();
        vapp.setState(stage.getApplianceStage().getState());
        vapp.setSubState(stage.getApplianceStage().getSubstate());

        virtualApplianceGenerator.addAuxiliaryEntitiesToPersist(vapp, entitiesToPersist);
        entitiesToPersist.add(vapp);

        // Create virtual machines and nodes
        for (VirtualMachineStage virtualMachineStage : stage.getMachineStages())
        {
            VirtualMachine vm = vmGenerator.createInstance(hypervisor);

            vm.setHypervisor(hypervisor);
            vm.setEnterprise(enterprise);
            vm.setName(virtualMachineStage.getName());
            vm.setState(virtualMachineStage.getState());

            vmGenerator.addAuxiliaryEntitiesToPersist(vm, entitiesToPersist);
            entitiesToPersist.add(vm);

            NodeVirtualImage node = nodeVirtualImageGenerator.createInstance(vapp, vm);
            nodeVirtualImageGenerator.addAuxiliaryEntitiesToPersist(node, entitiesToPersist);
            entitiesToPersist.add(node);

            vapp.addToNodeVirtualImages(node);
        }

        // Persist all entities
        setup(entitiesToPersist.toArray());

        // Testing stuff
        EntityManager manager = getEntityManagerWithAnActiveTransaction();
        EventingProcessor processor = getEventingProcessor(manager);

        try
        {
            for (VirtualMachineStage virtualMachineStage : stage.getMachineStages())
            {
                if (virtualMachineStage.getEvent() == null)
                {
                    continue;
                }

                VirtualMachine vm =
                    processor.repo.findVirtualMachineByName(virtualMachineStage.getName());

                processor.onEvent(buildEvent(virtualMachineStage.getEvent(), vm.getName(), vm
                    .getHypervisor().getMachine().getName(), vm.getHypervisor().getType().name()));

                vm = processor.repo.findVirtualMachineByName(virtualMachineStage.getName());

                assertEquals(vm.getState(), virtualMachineStage.getExpected());
            }

            vapp = processor.repo.findVirtualApplianceById(vapp.getId());

            assertEquals(vapp.getState(), stage.getApplianceStage().getExpectedState());
            assertEquals(vapp.getSubState(), stage.getApplianceStage().getExpectedSubstate());
        }
        finally
        {
            EntityManagerHelper.commit(manager);
        }
    }

    protected VirtualSystemEvent buildEvent(String event, String virtualMachine,
        String machineAddress, String machineType)
    {
        VirtualSystemEvent notification = new VirtualSystemEvent();

        notification.setEventType(event);
        notification.setVirtualSystemAddress(machineAddress);
        notification.setVirtualSystemId(virtualMachine);
        notification.setVirtualSystemType(machineType);

        return notification;
    }
}
