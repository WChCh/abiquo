package com.abiquo.tarantino.plugins.hyperv.utils;
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


import org.jinterop.dcom.impls.automation.IJIDispatch;

import com.hyper9.jwbem.SWbemObject;
import com.hyper9.jwbem.SWbemServices;

/**
 * Represents the startup configuration of a Windows-based process. The class is defined as a method
 * type definition, which means that it is only used for passing information to the Create method of
 * the Win32_Process class
 * 
 * @author pnavarro
 */
public class Win32ProcessStartup extends SWbemObject
{

    public Win32ProcessStartup(IJIDispatch objectDispatcher, SWbemServices service)
    {
        super(objectDispatcher, service);
    }

}
