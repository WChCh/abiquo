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

package com.abiquo.appliancemanager.transport;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(name = "OVFPackageInstanceStatus")
public class OVFPackageInstanceStatusDto
{
    // @XmlAttribute(name = "OVFId", namespace =
    // "http://www.abiquo.com/model/transport/appliancemanager", required = true)
    protected String ovfId;

    // @XmlAttribute(name = "OVFPackageStatus", namespace =
    // "http://www.abiquo.com/model/transport/appliancemanager", required = true)
    protected OVFPackageInstanceStatusType ovfPackageStatus;

    // @XmlAttribute(namespace = "http://www.abiquo.com/model/transport/appliancemanager", required
    // = false)
    protected Double progress;

    // @XmlAttribute(namespace = "http://www.abiquo.com/model/transport/appliancemanager", required
    // = false)
    protected String errorCause;

    public String getOvfId()
    {
        return ovfId;
    }

    public void setOvfId(String ovfId)
    {
        this.ovfId = ovfId;
    }

    public OVFPackageInstanceStatusType getOvfPackageStatus()
    {
        return ovfPackageStatus;
    }

    public void setOvfPackageStatus(OVFPackageInstanceStatusType ovfPackageStatus)
    {
        this.ovfPackageStatus = ovfPackageStatus;
    }

    public Double getProgress()
    {
        return progress;
    }

    public void setProgress(Double progress)
    {
        this.progress = progress;
    }

    public String getErrorCause()
    {
        return errorCause;
    }

    public void setErrorCause(String errorCause)
    {
        this.errorCause = errorCause;
    }

}
