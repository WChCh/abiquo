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

package com.abiquo.abiserver.business.hibernate.pojohb.virtualappliance;

// Generated 16-oct-2008 16:52:14 by Hibernate Tools 3.2.1.GA

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.abiserver.business.hibernate.pojohb.IPojoHB;
import com.abiquo.abiserver.business.hibernate.pojohb.infrastructure.DatastoreHB;
import com.abiquo.abiserver.business.hibernate.pojohb.infrastructure.HypervisorHB;
import com.abiquo.abiserver.business.hibernate.pojohb.infrastructure.StateEnum;
import com.abiquo.abiserver.business.hibernate.pojohb.user.EnterpriseHB;
import com.abiquo.abiserver.business.hibernate.pojohb.user.UserHB;
import com.abiquo.abiserver.business.hibernate.pojohb.virtualhardware.ResourceAllocationSettingData;
import com.abiquo.abiserver.business.hibernate.pojohb.virtualhardware.ResourceManagementHB;
import com.abiquo.abiserver.business.hibernate.pojohb.virtualimage.VirtualImageConversionsHB;
import com.abiquo.abiserver.business.hibernate.pojohb.virtualimage.VirtualimageHB;
import com.abiquo.abiserver.pojo.infrastructure.State;
import com.abiquo.abiserver.pojo.infrastructure.VirtualMachine;

/**
 * Virtualmachine generated by hbm2java
 */
public class VirtualmachineHB implements java.io.Serializable, IPojoHB<VirtualMachine>
{
    private final static Logger logger = LoggerFactory.getLogger(VirtualmachineHB.class);

    private static final long serialVersionUID = 8024546400260681298L;

    /**
     * 0 - if The virtualMachine is not created and managed by abiCloud
     */
    public final static int NOT_MANAGED_VM = 0;

    /**
     * 1 - if The virtualMachine is created and managed by abiCloud
     */
    public final static int MANAGED_VM = 1;

    private Integer idVm;

    private HypervisorHB hypervisor;

    private StateEnum state;

    private StateEnum subState;

    private VirtualimageHB image;

    private String uuid;

    private String name;

    private String description;

    private Integer ram;

    private Integer cpu;

    private Long hd;

    private Integer vdrpPort;

    private String vdrpIp;

    private int highDisponibility;

    private VirtualImageConversionsHB conversion;

    private final List<ResourceAllocationSettingData> rasds =
        new ArrayList<ResourceAllocationSettingData>();

    private Set<ResourceManagementHB> resman = new HashSet<ResourceManagementHB>();

    private int idType;

    private UserHB userHB;

    private EnterpriseHB enterpriseHB;

    private DatastoreHB datastore;

    private String password;

    public VirtualmachineHB()
    {
    }

    public Integer getIdVm()
    {
        return idVm;
    }

    public void setIdVm(final Integer idVm)
    {
        this.idVm = idVm;
    }

    public StateEnum getState()
    {
        return state;
    }

    public void setState(final StateEnum state)
    {
        this.state = state;
    }

    public StateEnum getSubState()
    {
        return subState;
    }

    public void setSubState(final StateEnum subState)
    {
        this.subState = subState;
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(final String uuid)
    {
        this.uuid = uuid;
    }

    public String getName()
    {
        return name;
    }

    public List<ResourceAllocationSettingData> getRasds()
    {
        // Cleaning the list
        rasds.clear();
        Set<ResourceManagementHB> resMans = getResman();
        for (ResourceManagementHB resMan : resMans)
        {
            if (resMan.checkResourceCoherency())
            {
                rasds.add(resMan.getRasd());
            }
            else
            {
                logger.error("concurrency is false");
            }
        }
        return rasds;
    }

    public void setName(String name)
    {
        if (!name.startsWith("ABQ_")
            && !(state == StateEnum.RUNNING || state == StateEnum.IN_PROGRESS) && isManaged())
        {
            name = "ABQ_" + name;
        }

        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(final String description)
    {
        this.description = description;
    }

    public Integer getRam()
    {
        return ram;
    }

    public void setRam(final Integer ram)
    {
        this.ram = ram;
    }

    public Integer getCpu()
    {
        return cpu;
    }

    public void setCpu(final Integer cpu)
    {
        this.cpu = cpu;
    }

    public Long getHd()
    {
        return hd;
    }

    public void setHd(final Long hd)
    {
        this.hd = hd;
    }

    public Integer getVdrpPort()
    {
        return vdrpPort;
    }

    public void setVdrpPort(final Integer vdrpPort)
    {
        this.vdrpPort = vdrpPort;
    }

    public String getVdrpIp()
    {
        return vdrpIp;
    }

    public void setVdrpIp(final String vdrpIp)
    {
        this.vdrpIp = vdrpIp;
    }

    public int getHighDisponibility()
    {
        return highDisponibility;
    }

    public void setHighDisponibility(final int highDisponibility)
    {
        this.highDisponibility = highDisponibility;
    }

    public Set<ResourceManagementHB> getResman()
    {
        return resman;
    }

    public void setResman(final Set<ResourceManagementHB> resman)
    {
        this.resman = resman;
    }

    public VirtualimageHB getImage()
    {
        return image;
    }

    public void setImage(final VirtualimageHB image)
    {
        this.image = image;
    }

    public HypervisorHB getHypervisor()
    {
        return hypervisor;
    }

    public void setHypervisor(final HypervisorHB hypervisor)
    {
        this.hypervisor = hypervisor;
    }

    public int getIdType()
    {
        return idType;
    }

    public void setIdType(final int idType)
    {
        this.idType = idType;
    }

    public UserHB getUserHB()
    {
        return userHB;
    }

    public void setUserHB(final UserHB userHB)
    {
        this.userHB = userHB;
    }

    public EnterpriseHB getEnterpriseHB()
    {
        return enterpriseHB;
    }

    public void setEnterpriseHB(final EnterpriseHB enterpriseHB)
    {
        this.enterpriseHB = enterpriseHB;
    }

    @Override
    public VirtualMachine toPojo()
    {
        VirtualMachine virtualMachine = new VirtualMachine();

        virtualMachine.setId(idVm);
        virtualMachine.setName(name);

        if (hypervisor == null)
        {
            virtualMachine.setAssignedTo(null);
        }
        else
        {
            virtualMachine.setAssignedTo(hypervisor.toPojo());
        }

        if (conversion == null)
        {
            virtualMachine.setConversion(null);
        }
        else
        {
            virtualMachine.setConversion(conversion.toPojo());
        }

        virtualMachine.setVirtualImage(image == null ? null : image.toPojo());
        virtualMachine.setUUID(uuid);
        virtualMachine.setDescription(description);
        virtualMachine.setRam(ram);
        virtualMachine.setCpu(cpu);
        virtualMachine.setHd(hd);
        virtualMachine.setVdrpIP(vdrpIp);
        virtualMachine.setVdrpPort(vdrpPort);
        virtualMachine.setState(new State(state));
        virtualMachine.setSubState(new State(subState));
        virtualMachine.setHighDisponibility(highDisponibility == 1 ? true : false);
        virtualMachine.setUser(userHB == null ? null : userHB.toPojo());
        virtualMachine.setEnterprise(enterpriseHB == null ? null : enterpriseHB.toPojo());
        virtualMachine.setIdType(idType);
        virtualMachine.setDatastore(datastore == null ? null : datastore.toPojo());
        virtualMachine.setPassword(password);

        return virtualMachine;
    }

    /**
     * @return the conversions
     */
    public VirtualImageConversionsHB getConversion()
    {
        return conversion;
    }

    /**
     * @param conversions the conversions to set
     */
    public void setConversion(final VirtualImageConversionsHB conversion)
    {
        this.conversion = conversion;
    }

    /**
     * Utility method to check if the virtual machine is managed by abicloud
     * 
     * @return true if it's managed, false otherwise
     */
    public boolean isManaged()
    {
        return getImage() != null && getImage().getRepository() != null;
    }

    public void setDatastore(final DatastoreHB datastore)
    {
        this.datastore = datastore;
    }

    public DatastoreHB getDatastore()
    {
        return datastore;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(final String password)
    {
        this.password = password;
    }

}
