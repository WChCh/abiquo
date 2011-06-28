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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.abiquo.api.tracer.TracerLogger;
import com.abiquo.commons.amqp.impl.vsm.VSMCallback;
import com.abiquo.commons.amqp.impl.vsm.domain.VirtualSystemEvent;
import com.abiquo.server.core.cloud.NodeVirtualImage;
import com.abiquo.server.core.cloud.State;
import com.abiquo.server.core.cloud.VirtualAppliance;
import com.abiquo.server.core.cloud.VirtualDatacenterRep;
import com.abiquo.server.core.cloud.VirtualMachine;
import com.abiquo.tracer.ComponentType;
import com.abiquo.tracer.EventType;
import com.abiquo.tracer.SeverityType;
import com.abiquo.vsm.events.VMEventType;

/**
 * This listener {@link VSMCallback} receives the events from the all Virtual System Monitors in
 * each datacenter and updates the state of virtual machines and virtual appliances in database.
 * 
 * @author eruiz@abiquo.com
 */
@Service
@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
public class EventingProcessor implements VSMCallback
{
    private final static Logger LOGGER = LoggerFactory.getLogger(EventingProcessor.class);

    @Autowired
    protected VirtualDatacenterRep repo;

    @Autowired
    protected TracerLogger tracer;

    /** Event to virtual machine state translations */
    protected final Map<VMEventType, State> stateFromEvent = new HashMap<VMEventType, State>()
    {
        {
            put(VMEventType.POWER_OFF, State.POWERED_OFF);
            put(VMEventType.POWER_ON, State.RUNNING);
            put(VMEventType.PAUSED, State.PAUSED);
            put(VMEventType.RESUMED, State.RUNNING);
            put(VMEventType.DESTROYED, State.NOT_DEPLOYED);
            put(VMEventType.MOVED, State.MOVING);
        }
    };

    /** Considered deployed states in a virtual machine */
    protected final Set<State> deployedStates = new HashSet<State>()
    {
        {
            add(State.PAUSED);
            add(State.POWERED_OFF);
            add(State.REBOOTED);
            add(State.RUNNING);
        }
    };

    /** Event to log-event virtual machine translations */
    protected final Map<VMEventType, EventType> logEventFromEvent =
        new HashMap<VMEventType, EventType>()
        {
            {
                put(VMEventType.POWER_OFF, EventType.VM_POWEROFF);
                put(VMEventType.POWER_ON, EventType.VM_POWERON);
                put(VMEventType.PAUSED, EventType.VM_PAUSED);
                put(VMEventType.RESUMED, EventType.VM_RESUMED);
                put(VMEventType.DESTROYED, EventType.VM_DESTROY);
                put(VMEventType.MOVED, EventType.VM_MOVED);
            }
        };

    /**
     * Constructor for test purposes only.
     * 
     * @param em The entity manager to use.
     */
    public EventingProcessor(EntityManager em)
    {
        this.repo = new VirtualDatacenterRep(em);
        this.tracer = new TracerLogger();
    }

    public EventingProcessor()
    {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onEvent(VirtualSystemEvent notification)
    {
        LOGGER.debug("New notification received, {}.", notification.toString());

        // Check if notification contains a valid event
        VMEventType event = eventFromString(notification.getEventType());

        if (stateFromEvent.get(event) == null)
        {
            LOGGER.error("Unknown event {}. Just ignoring it.", notification.getEventType());
            return;
        }

        // Update virtual machine state
        VirtualMachine machine = repo.findVirtualMachineByName(notification.getVirtualSystemId());

        if (machine != null)
        {
            repo.update(updateMachineState(machine, notification));
        }

        // Update virtual appliance state
        VirtualAppliance appliance = repo.findVirtualApplianceByVirtualMachine(machine);

        if (appliance != null)
        {
            repo.updateVirtualAppliance(updateVirtualApplianceState(appliance));
        }
    }

    /**
     * Process the given notification and if it affects to the virtual machine, updates the state of
     * a virtual machine instance.
     * 
     * @param machine The instance to update.
     * @param notification The notification.
     * @return The virtual machine instance.
     */
    protected VirtualMachine updateMachineState(VirtualMachine machine,
        VirtualSystemEvent notification)
    {
        VMEventType event = eventFromString(notification.getEventType());

        if (machine.getState() == State.HA_IN_PROGRESS)
        {
            LOGGER.debug(
                "Virtual machine {} is being migrated by HA engine. Just ignoring the {} event.",
                machine.getName(), event.toString().toUpperCase());
            return machine;
        }

        return processEvent(machine, event, notification);
    }

    /**
     * Process the given notification and if it affects to the virtual machine, updates the state of
     * a virtual machine instance.
     * 
     * @param machine The instance to update.
     * @param event The event notified.
     * @param notification The complete notification.
     * @return The virtual machine instance.
     */
    protected VirtualMachine processEvent(VirtualMachine machine, VMEventType event,
        VirtualSystemEvent notification)
    {
        switch (event)
        {
            case PAUSED:
            case POWER_OFF:
            case POWER_ON:
            case RESUMED:
            case SAVED:
                machine.setState(stateFromEvent.get(event));

                String message =
                    String.format(
                        "Processed %s event in machine %s, the current machine state is %s.",
                        event.name(), machine.getName(), machine.getState().name());

                traceVirtualMachineStateUpdated(notification, message);
                LOGGER.debug(message);

                break;

            default:
                LOGGER.warn("Ignoring {} event.", event);
                break;
        }

        return machine;
    }

    /**
     * Updates the state and substate of a virtual appliance instance looking at the state of the
     * attached virtual machines.
     * 
     * @param appliance The instance to update.
     * @return The virtual appliance instance.
     */
    protected VirtualAppliance updateVirtualApplianceState(VirtualAppliance appliance)
    {
        State state = appliance.getState();
        State substate = appliance.getSubState();

        // Bundle logic handles the state of the virtual appliance
        if (!bundlingAppliance(appliance))
        {
            if (!inProgress(appliance) && !hasNodes(appliance))
            {
                // Enterprise case, DESTROYED event
                appliance.setState(State.NOT_DEPLOYED);
                appliance.setSubState(State.NOT_DEPLOYED);
            }
            else if (allNodesDeployed(appliance))
            {
                appliance.setState(State.RUNNING);
                appliance.setSubState(State.RUNNING);
            }
            else if (!inProgress(appliance))
            {
                // Enterprise case, DESTROYED event
                appliance.setState(State.APPLY_CHANGES_NEEDED);
                appliance.setSubState(State.APPLY_CHANGES_NEEDED);
            }
        }

        if (state != appliance.getState() || substate != appliance.getSubState())
        {
            String message =
                String.format("Virtual appliance %s state has been updated from %s/%s to %s/%s",
                    appliance.getName(), state.toString(), substate.toString(), appliance
                        .getState().toString(), appliance.getSubState().toString());

            traceVirtualApplianceStateUpdated(message);
            LOGGER.debug(message);
        }

        return appliance;
    }

    /**
     * Helper method to check if a virtual appliance state is IN_PROGRESS.
     * 
     * @param appliance The virtual appliance to check.
     * @return True if the virtual appliance state is IN_PROGESS. False otherwise.
     */
    protected boolean inProgress(VirtualAppliance appliance)
    {
        return (appliance.getState() == State.IN_PROGRESS);
    }

    /**
     * Helper method to check if a virtual appliance has some node attached.
     * 
     * @param appliance The virtual appliance to check.
     * @return True if the virtual appliance has some node attached. False otherwise.
     */
    protected boolean hasNodes(VirtualAppliance appliance)
    {
        return !appliance.getNodes().isEmpty();
    }

    /**
     * Helper method to check if a virtual appliance state is IN_PROGRESS ans substate is BUNDLING.
     * 
     * @param appliance The virtual appliance to check.
     * @return True if the virtual appliance state is IN_PROGESS and substate is BUNDLING. False
     *         otherwise.
     */
    protected boolean bundlingAppliance(VirtualAppliance appliance)
    {
        return (inProgress(appliance) && appliance.getSubState() == State.BUNDLING);
    }

    /**
     * Helper method to check if a virtual appliance has all nodes in a deployed state.
     * 
     * @param appliance The virtual appliance to check.
     * @return True if the virtual appliance has all nodes in a deployed state. False otherwise.
     */
    protected boolean allNodesDeployed(VirtualAppliance appliance)
    {
        for (NodeVirtualImage node : appliance.getNodes())
        {
            if (!deployedStates.contains(node.getVirtualMachine().getState()))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Publish an INFO system log to tracer system with VIRTUAL_MACHINE component type.
     * 
     * @param notification The received notification.
     * @param message The message to publish
     */
    protected void traceVirtualMachineStateUpdated(final VirtualSystemEvent notification,
        final String message)
    {
        VMEventType event = eventFromString(notification.getEventType());

        if (logEventFromEvent.containsKey(event))
        {
            tracer.systemLog(SeverityType.INFO, ComponentType.VIRTUAL_MACHINE,
                logEventFromEvent.get(event), message);
        }
    }

    /**
     * Publish an INFO system log to tracer system with VIRTUAL_APPLIANCE component type.
     * 
     * @param message The message to publish
     */
    protected void traceVirtualApplianceStateUpdated(final String message)
    {
        // TODO use a correct EventType
        tracer.systemLog(SeverityType.INFO, ComponentType.VIRTUAL_APPLIANCE,
            EventType.VAPP_REFRESH, message);
    }

    protected VMEventType eventFromString(final String name)
    {
        try
        {
            return VMEventType.valueOf(name);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }
}
