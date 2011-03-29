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

package com.abiquo.commons.amqp.impl.datacenter.domain.builder;

import com.abiquo.commons.amqp.impl.datacenter.domain.DiskStandard;
import com.abiquo.commons.amqp.impl.datacenter.domain.DiskStateful;
import com.abiquo.commons.amqp.impl.datacenter.domain.jobs.SnapshotVirtualMachine;
import com.abiquo.commons.amqp.impl.datacenter.domain.jobs.SnapshotVirtualMachine.SourceDisk;

public class SnapshotVirtualMachineJobBuilder extends VirtualFactoryJobBuilder
{

    private SourceDisk source;

    private DiskStandard destination;

    public SnapshotVirtualMachineJobBuilder connection(String hypervisorID, String hypervisortype,
        String ip, String port, String protocol, String loginUser, String loginPasswoed)
    {
        super
            .connection(hypervisorID, hypervisortype, ip, port, protocol, loginUser, loginPasswoed);
        return this;
    }

    public SnapshotVirtualMachineJobBuilder source(String virtualMachineId, String format,
        String capacity, String datastore, String path)
    {

        source = new SourceDisk();

        DiskStandard disk = new DiskStandard();
        disk.setDiskID(virtualMachineId);
        disk.setFormat(format);
        disk.setCapacity(capacity);
        disk.setDatastore(datastore);
        disk.setPath(path);

        source.setDiskStandard(disk);

        return this;
    }

    public SnapshotVirtualMachineJobBuilder source(String virtualMachineId, String format,
        String capacity, String iqn)
    {

        source = new SourceDisk();

        DiskStateful disk = new DiskStateful();
        disk.setDiskID(virtualMachineId);
        disk.setFormat(format);
        disk.setCapacity(capacity);
        disk.setIqn(iqn);

        source.setDiskStatefull(disk);

        return this;
    }

    public SnapshotVirtualMachineJobBuilder destination(String format, String capacity,
        String datastore, String path)
    {

        destination = new DiskStandard();

        destination.setFormat(format);
        destination.setCapacity(capacity);
        destination.setDatastore(datastore);
        destination.setPath(path);

        return this;
    }

    public SnapshotVirtualMachine build()
    {
        SnapshotVirtualMachine sn = new SnapshotVirtualMachine();
        sn.setHypervisorConnection(connection);
        sn.setSourceDisk(source);
        sn.setDestinationDisk(destination);
        return sn;
    }

}
