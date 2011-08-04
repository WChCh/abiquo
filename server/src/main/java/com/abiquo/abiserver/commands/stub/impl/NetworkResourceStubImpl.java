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

/**
 * 
 */
package com.abiquo.abiserver.commands.stub.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.wink.client.ClientResponse;

import com.abiquo.abiserver.business.hibernate.pojohb.user.UserHB;
import com.abiquo.abiserver.commands.stub.AbstractAPIStub;
import com.abiquo.abiserver.commands.stub.NetworkResourceStub;
import com.abiquo.abiserver.exception.NetworkCommandException;
import com.abiquo.abiserver.networking.IPAddress;
import com.abiquo.abiserver.networking.IPNetworkRang;
import com.abiquo.abiserver.pojo.authentication.UserSession;
import com.abiquo.abiserver.pojo.networking.IpPoolManagement;
import com.abiquo.abiserver.pojo.networking.NetworkConfiguration;
import com.abiquo.abiserver.pojo.networking.VlanNetwork;
import com.abiquo.abiserver.pojo.result.BasicResult;
import com.abiquo.abiserver.pojo.result.DataResult;
import com.abiquo.abiserver.pojo.result.ListResponse;
import com.abiquo.abiserver.pojo.user.Enterprise;
import com.abiquo.model.rest.RESTLink;
import com.abiquo.server.core.enterprise.EnterpriseDto;
import com.abiquo.server.core.enterprise.EnterprisesDto;
import com.abiquo.server.core.infrastructure.network.IpPoolManagementDto;
import com.abiquo.server.core.infrastructure.network.IpsPoolManagementDto;
import com.abiquo.server.core.infrastructure.network.VLANNetworkDto;
import com.abiquo.server.core.infrastructure.network.VLANNetworksDto;
import com.abiquo.server.core.infrastructure.network.VMNetworkConfigurationDto;
import com.abiquo.server.core.infrastructure.network.VMNetworkConfigurationsDto;
import com.abiquo.server.core.infrastructure.network.VlanTagAvailabilityDto;

/**
 * @author jdevesa
 */
public class NetworkResourceStubImpl extends AbstractAPIStub implements NetworkResourceStub
{

    @SuppressWarnings("unchecked")
    @Override
    public DataResult<Boolean> checkVLANTagAvailability(final Integer datacenterId,
        final Integer proposedVLANTag, final Integer currentVlanId)
    {
        DataResult<Boolean> result = new DataResult<Boolean>();
        String uri = createDatacenterPublicTagCheck(datacenterId);
        UserHB user = getCurrentUser();
        ClientResponse response =
            resource(uri, user.getUser(), user.getPassword()).queryParam("tag", proposedVLANTag)
                .get();

        if (response.getStatusCode() == 200)
        {
            VlanTagAvailabilityDto availDto = response.getEntity(VlanTagAvailabilityDto.class);
            switch (availDto.getAvailable())
            {
                case AVAILABLE:
                {
                    result.setData(Boolean.TRUE);
                    break;
                }
                case INVALID:
                {
                    result.setData(Boolean.FALSE);
                    result.setMessage(availDto.getMessage());
                    break;
                }
                case USED:
                {
                    // If its used, check if it is used by the currentVlanId network
                    if (currentVlanId != null)
                    {
                        DataResult<VlanNetwork> dr =
                            (DataResult<VlanNetwork>) getPublicNetwork(datacenterId, currentVlanId);
                        if (dr.getData().getVlanTag().equals(proposedVLANTag))
                        {
                            result.setData(Boolean.TRUE);
                        }
                        else
                        {
                            result.setData(Boolean.FALSE);
                            result.setMessage(availDto.getMessage());
                        }
                    }
                    else
                    {
                        result.setData(Boolean.FALSE);
                        result.setMessage(availDto.getMessage());
                    }
                }
            }
            result.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, result, "checkVLANTagAvailability");
        }

        return result;
    }

    @Override
    public BasicResult createPrivateVlan(final UserSession userSession, final Integer vdcId,
        final VLANNetworkDto dto)
    {
        DataResult<VlanNetwork> result = new DataResult<VlanNetwork>();
        String uri = createPrivateNetworksLink(vdcId);
        ClientResponse response = post(uri, dto);

        if (response.getStatusCode() == 201)
        {
            VLANNetworkDto networkDto = response.getEntity(VLANNetworkDto.class);
            result.setData(createFlexObject(networkDto));
            result.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, result, "createPrivateVLANNetwork");
        }

        return result;
    }

    @Override
    public BasicResult createPublicVlan(final Integer datacenterId, final String networkName,
        final Integer vlanTag, final NetworkConfiguration configuration, final Enterprise enterprise)
    {
        DataResult<VlanNetwork> result = new DataResult<VlanNetwork>();
        String uri = createPublicNetworksLink(datacenterId);

        VLANNetworkDto dto = new VLANNetworkDto();
        dto.setAddress(configuration.getNetworkAddress());
        dto.setDefaultNetwork(Boolean.FALSE);
        dto.setGateway(configuration.getGateway());
        dto.setMask(configuration.getMask());
        dto.setName(networkName);
        dto.setPrimaryDNS(configuration.getPrimaryDNS());
        dto.setSecondaryDNS(configuration.getSecondaryDNS());
        dto.setSufixDNS(configuration.getSufixDNS());
        dto.setTag(vlanTag);

        ClientResponse response = post(uri, dto);
        if (response.getStatusCode() == 201)
        {
            result.setData(createFlexObject(response.getEntity(VLANNetworkDto.class)));
            result.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, result, "createPublicVlan");
        }
        return result;
    }

    @Override
    public BasicResult deletePrivateVlan(final Integer vdcId, final Integer vlanId)
    {
        BasicResult result = new BasicResult();

        String uri = createPrivateNetworkLink(vdcId, vlanId);
        ClientResponse response = delete(uri);
        if (response.getStatusCode() == 204)
        {
            result.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, result, "deletePrivateVlan");
        }
        return result;
    }

    @Override
    public BasicResult deletePublicVlan(final Integer datacenterId, final Integer vlanId)
    {
        BasicResult result = new BasicResult();

        String uri = createPublicNetworkLink(datacenterId, vlanId);
        ClientResponse response = delete(uri);
        if (response.getStatusCode() == 204)
        {
            result.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, result, "deletePublicVlan");
        }
        return result;
    }

    @Override
    public BasicResult editPrivateVlan(final Integer vdcId, final Integer vlanId,
        final VLANNetworkDto vlandto)
    {
        DataResult<VlanNetwork> result = new DataResult<VlanNetwork>();
        String uri = createPrivateNetworkLink(vdcId, vlanId);
        ClientResponse response = put(uri, vlandto);

        if (response.getStatusCode() == 200)
        {
            result.setData(createFlexObject(response.getEntity(VLANNetworkDto.class)));
            result.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, result, "editPublicIp");
        }

        return result;
    }

    @Override
    public BasicResult editPublicIp(final Integer datacenterId, final Integer vlanId,
        final Integer idManagement, final IpPoolManagement ipPoolManagement)
    {
        BasicResult result = new BasicResult();
        String uri = createPublicNetworkIPLink(datacenterId, vlanId, idManagement);
        ClientResponse response = put(uri, createDtoObject(ipPoolManagement));

        if (response.getStatusCode() == 200)
        {
            result.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, result, "editPublicIp");
        }

        return result;
    }

    @Override
    public BasicResult editPublicIps(final Integer datacenterId, final Integer vlanNetworkId,
        final ArrayList<IpPoolManagement> listOfPublicIPs)
    {
        BasicResult result = new BasicResult();
        String uri = createPublicNetworkIPsLink(datacenterId, vlanNetworkId);

        IpsPoolManagementDto ipsDto = new IpsPoolManagementDto();
        for (IpPoolManagement ipPool : listOfPublicIPs)
        {
            ipsDto.add(this.createDtoObject(ipPool));
        }

        ClientResponse response = put(uri, ipsDto);
        if (response.getStatusCode() == 204)
        {
            result.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, result, "editPublicIps");
        }
        return result;
    }

    @Override
    public BasicResult editPublicVlan(final Integer datacenterId, final Integer vlanNetworkId,
        final String vlanName, final Integer vlanTag, final NetworkConfiguration configuration,
        final Boolean defaultNetwork, final Enterprise enterprise)
    {
        BasicResult result = new BasicResult();
        String uri = createPublicNetworkLink(datacenterId, vlanNetworkId);

        VLANNetworkDto dto = new VLANNetworkDto();
        dto.setAddress(configuration.getNetworkAddress());
        dto.setDefaultNetwork(defaultNetwork);
        dto.setGateway(configuration.getGateway());
        dto.setId(vlanNetworkId);
        dto.setMask(configuration.getMask());
        dto.setName(vlanName);
        dto.setPrimaryDNS(configuration.getPrimaryDNS());
        dto.setSecondaryDNS(configuration.getSecondaryDNS());
        dto.setSufixDNS(configuration.getSufixDNS());
        dto.setTag(vlanTag);

        ClientResponse response = put(uri, dto);
        if (response.getStatusCode() == 200)
        {
            result.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, result, "editPublicVlan");
        }
        return result;
    }

    @Override
    public BasicResult getEnterpriseFromReservedVlanId(final Integer datacenterId,
        final Integer vlanId)
    {
        // TODO: per a interroute aplicar aixo.
        DataResult<Enterprise> dr = new DataResult<Enterprise>();
        dr.setSuccess(Boolean.TRUE);
        dr.setData(null);

        return dr;
    }

    @Override
    public BasicResult getEnterprisesWithNetworksByDatacenter(final UserSession userSession,
        final Integer datacenterId, final Integer offset, final Integer numElem, final String ipLike)
        throws NetworkCommandException
    {
        DataResult<ListResponse<Enterprise>> dataResult =
            new DataResult<ListResponse<Enterprise>>();

        List<Enterprise> listEnt = new ArrayList<Enterprise>();

        StringBuilder buildRequest = new StringBuilder(createDatacenterLink(datacenterId));
        buildRequest.append("/action/enterprises");
        buildRequest.append("?network=true");
        buildRequest.append("&startwith=" + offset);
        buildRequest.append("&limit=" + numElem);

        ClientResponse response = get(buildRequest.toString());

        if (response.getStatusCode() == 200)
        {
            EnterprisesDto enterprises = response.getEntity(EnterprisesDto.class);
            for (EnterpriseDto entdto : enterprises.getCollection())
            {

                Enterprise e = Enterprise.create(entdto);
                listEnt.add(e);
            }
            ListResponse<Enterprise> listResponse = new ListResponse<Enterprise>();
            listResponse.setList(listEnt);
            listResponse.setTotalNumEntities(listEnt.size());

            dataResult.setData(listResponse);
            dataResult.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, dataResult, "getEnterprisesWithNetworksByDatacenter");
        }
        return dataResult;

    }

    @Override
    public BasicResult getGatewayByVirtualMachine(final Integer vdcId, final Integer vappId,
        final Integer vmId)
    {
        DataResult<IPAddress> result = new DataResult<IPAddress>();
        String uri = createVirtualMachineConfigurationsLink(vdcId, vappId, vmId);

        ClientResponse response = get(uri);
        if (response.getStatusCode() == 200)
        {
            VMNetworkConfigurationsDto dtos = response.getEntity(VMNetworkConfigurationsDto.class);
            for (VMNetworkConfigurationDto dto : dtos.getCollection())
            {
                if (dto.getUsed())
                {
                    result.setData(IPAddress.newIPAddress(dto.getGateway()));
                    result.setSuccess(Boolean.TRUE);
                    return result;
                }
            }

            // Unknown exception. At least one result should be returned before.
            result.setSuccess(Boolean.FALSE);
            result
                .setMessage("Unknown exception while retrieving the gateway of the virtual machine "
                    + vmId);
        }
        else
        {
            populateErrors(response, result, "getGatewayByVirtualMachine");
        }
        return result;
    }

    @Override
    public BasicResult getGatewayListByVirtualMachine(final Integer vdcId, final Integer vappId,
        final Integer vmId)
    {
        DataResult<List<IPAddress>> result = new DataResult<List<IPAddress>>();
        List<IPAddress> gateways = new ArrayList<IPAddress>();
        String uri = createVirtualMachineConfigurationsLink(vdcId, vappId, vmId);

        ClientResponse response = get(uri);
        if (response.getStatusCode() == 200)
        {
            VMNetworkConfigurationsDto dtos = response.getEntity(VMNetworkConfigurationsDto.class);
            for (VMNetworkConfigurationDto dto : dtos.getCollection())
            {
                gateways.add(IPAddress.newIPAddress(dto.getGateway()));
            }
            result.setData(gateways);
            result.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, result, "getGatewayListByVirtualMachine");
        }
        return result;
    }

    @Override
    public BasicResult getInfoDHCPServer(final UserSession userSession, final Integer vdcId)
        throws NetworkCommandException
    {
        DataResult<String> dataResult = new DataResult<String>();
        StringBuilder buildRequest = new StringBuilder(createVirtualDatacentersLink());
        buildRequest.append("/" + vdcId.toString());
        buildRequest.append("/action/dhcpinfo");

        ClientResponse response = get(buildRequest.toString());
        if (response.getStatusCode() == 200)
        {
            String dhcpinfo = response.getEntity(String.class);
            dataResult.setData(dhcpinfo);
            dataResult.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, dataResult, "getInfoDHCPServer");
        }
        return dataResult;
    }

    @Override
    public BasicResult getListNetworkPoolByEnterprise(final Integer enterpriseId,
        final Integer offset, final Integer numElem, final String filterLike, final String orderBy,
        final Boolean asc) throws NetworkCommandException
    {
        DataResult<ListResponse<IpPoolManagement>> dataResult =
            new DataResult<ListResponse<IpPoolManagement>>();
        ListResponse<IpPoolManagement> listResponse = new ListResponse<IpPoolManagement>();

        StringBuilder buildRequest = new StringBuilder(createEnterpriseIPsLink(enterpriseId));
        buildRequest.append("?startwith=" + offset);
        buildRequest.append("&limit=" + numElem);
        buildRequest.append("&by=" + transformOrderBy(orderBy));
        buildRequest.append("&asc=" + (asc ? "true" : "false"));
        String filter = filterLike;
        try
        {
            filter = URLEncoder.encode(filterLike, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {

        }
        if (!filter.isEmpty())
        {
            buildRequest.append("&has=" + filter);
        }
        String request = buildRequest.toString();

        ClientResponse response = get(request);

        if (response.getStatusCode() == 200)
        {
            IpsPoolManagementDto ips = response.getEntity(IpsPoolManagementDto.class);
            List<IpPoolManagement> flexIps = new ArrayList<IpPoolManagement>();

            for (IpPoolManagementDto ip : ips.getCollection())
            {
                IpPoolManagement flexIp = createFlexObject(ip);
                flexIp.setEnterpriseId(enterpriseId);
                flexIps.add(flexIp);
            }
            listResponse.setList(flexIps);
            listResponse.setTotalNumEntities(ips.getTotalSize());

            dataResult.setData(listResponse);
            dataResult.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, dataResult, "getListNetworkPoolByEnterprise");
        }

        return dataResult;
    }

    @Override
    public BasicResult getListNetworkPoolByPrivateVLAN(final Integer vdcId, final Integer vlanId,
        final Integer offset, final Integer numberOfNodes, final String filterLike,
        final String orderBy, final Boolean asc, final Boolean onlyAvailable)
    {

        DataResult<ListResponse<IpPoolManagement>> dataResult =
            new DataResult<ListResponse<IpPoolManagement>>();
        ListResponse<IpPoolManagement> listResponse = new ListResponse<IpPoolManagement>();

        StringBuilder buildRequest = new StringBuilder(createPrivateNetworkIPsLink(vdcId, vlanId));
        buildRequest.append("?startwith=" + offset);
        buildRequest.append("&limit=" + numberOfNodes);
        buildRequest.append("&by=" + transformOrderBy(orderBy));
        buildRequest.append("&asc=" + (asc ? "true" : "false"));
        buildRequest.append("&onlyAvailable=" + (onlyAvailable ? "true" : "false"));
        if (!filterLike.isEmpty())
        {
            buildRequest.append("&has=" + filterLike);
        }

        ClientResponse response = get(buildRequest.toString());

        if (response.getStatusCode() == 200)
        {
            IpsPoolManagementDto ips = response.getEntity(IpsPoolManagementDto.class);
            List<IpPoolManagement> flexIps = new ArrayList<IpPoolManagement>();

            for (IpPoolManagementDto ip : ips.getCollection())
            {
                IpPoolManagement flexIp = createFlexObject(ip);
                flexIps.add(flexIp);
            }
            listResponse.setList(flexIps);
            listResponse.setTotalNumEntities(ips.getTotalSize());

            dataResult.setData(listResponse);
            dataResult.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, dataResult, "getListNetworkPoolByPrivateVLAN");
        }

        return dataResult;
    }

    @Override
    public BasicResult getListNetworkPoolByVirtualDatacenter(final Integer vdcId,
        final Integer offset, final Integer numElem, final String filterLike, final String orderBy,
        final Boolean asc) throws NetworkCommandException
    {
        DataResult<ListResponse<IpPoolManagement>> dataResult =
            new DataResult<ListResponse<IpPoolManagement>>();
        ListResponse<IpPoolManagement> listResponse = new ListResponse<IpPoolManagement>();

        StringBuilder buildRequest =
            new StringBuilder(createVirtualDatacenterPrivateIPsLink(vdcId));
        buildRequest.append("?startwith=" + offset);
        buildRequest.append("&limit=" + numElem);
        buildRequest.append("&by=" + transformOrderBy(orderBy));
        buildRequest.append("&asc=" + (asc ? "true" : "false"));
        if (!filterLike.isEmpty())
        {
            buildRequest.append("&has=" + filterLike);
        }

        ClientResponse response = get(buildRequest.toString());

        if (response.getStatusCode() == 200)
        {
            IpsPoolManagementDto ips = response.getEntity(IpsPoolManagementDto.class);
            List<IpPoolManagement> flexIps = new ArrayList<IpPoolManagement>();

            for (IpPoolManagementDto ip : ips.getCollection())
            {
                IpPoolManagement flexIp = createFlexObject(ip);
                flexIps.add(flexIp);
            }
            listResponse.setList(flexIps);
            listResponse.setTotalNumEntities(ips.getTotalSize());

            dataResult.setData(listResponse);
            dataResult.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, dataResult, "getListNetworkPoolByVirtualDatacenter");
        }

        return dataResult;
    }

    @Override
    public DataResult<ListResponse<IpPoolManagement>> getListNetworkPublicPoolByDatacenter(
        final Integer datacenterId, final Integer offset, final Integer numberOfNodes,
        final String filterLike, final String orderBy, final Boolean asc)
        throws NetworkCommandException
    {
        DataResult<ListResponse<IpPoolManagement>> dataResult =
            new DataResult<ListResponse<IpPoolManagement>>();
        ListResponse<IpPoolManagement> listResponse = new ListResponse<IpPoolManagement>();

        StringBuilder buildRequest = new StringBuilder(createDatacenterPublicIPsLink(datacenterId));
        buildRequest.append("?startwith=" + offset);
        buildRequest.append("&limit=" + numberOfNodes);
        buildRequest.append("&by=" + transformOrderBy(orderBy));
        buildRequest.append("&asc=" + (asc ? "true" : "false"));
        if (!filterLike.isEmpty())
        {
            buildRequest.append("&has=" + filterLike);
        }

        ClientResponse response = get(buildRequest.toString());

        if (response.getStatusCode() == 200)
        {
            IpsPoolManagementDto ips = response.getEntity(IpsPoolManagementDto.class);
            List<IpPoolManagement> flexIps = new ArrayList<IpPoolManagement>();

            for (IpPoolManagementDto ip : ips.getCollection())
            {
                IpPoolManagement flexIp = createFlexObject(ip);
                flexIps.add(flexIp);
            }
            listResponse.setList(flexIps);
            listResponse.setTotalNumEntities(ips.getTotalSize());

            dataResult.setData(listResponse);
            dataResult.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, dataResult, "getListNetworkPublicPoolByDatacenter");
        }

        return dataResult;
    }

    @Override
    public BasicResult getListNetworkPublicPoolByVlan(final Integer datacenterId,
        final Integer vlanId, final Integer offset, final Integer numberOfNodes,
        final String filterLike, final String orderBy, final Boolean asc, final Boolean all)
        throws NetworkCommandException
    {
        DataResult<ListResponse<IpPoolManagement>> dataResult =
            new DataResult<ListResponse<IpPoolManagement>>();
        ListResponse<IpPoolManagement> listResponse = new ListResponse<IpPoolManagement>();

        StringBuilder buildRequest =
            new StringBuilder(createPublicNetworkIPsLink(datacenterId, vlanId));
        buildRequest.append("?startwith=" + offset);
        buildRequest.append("&limit=" + numberOfNodes);
        buildRequest.append("&by=" + transformOrderBy(orderBy));
        buildRequest.append("&asc=" + (asc ? "true" : "false"));
        buildRequest.append("&all=" + (all ? "true" : "false"));
        if (!filterLike.isEmpty())
        {
            buildRequest.append("&has=" + filterLike);
        }

        ClientResponse response = get(buildRequest.toString());

        if (response.getStatusCode() == 200)
        {
            IpsPoolManagementDto ips = response.getEntity(IpsPoolManagementDto.class);
            List<IpPoolManagement> flexIps = new ArrayList<IpPoolManagement>();

            for (IpPoolManagementDto ip : ips.getCollection())
            {
                IpPoolManagement flexIp = createFlexObject(ip);
                flexIps.add(flexIp);
            }
            listResponse.setList(flexIps);
            listResponse.setTotalNumEntities(ips.getTotalSize());

            dataResult.setData(listResponse);
            dataResult.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, dataResult, "getListNetworkPublicPoolByVlan");
        }

        return dataResult;
    }

    @Override
    public BasicResult getListNetworkPublicPoolPurchasedByVirtualDatacenter(final Integer vdcId,
        final Boolean onlyAvailable, final Integer offset, final Integer numberOfNodes,
        final String filterLike, final String orderBy, final Boolean asc)
        throws NetworkCommandException
    {
        DataResult<ListResponse<IpPoolManagement>> dataResult =
            new DataResult<ListResponse<IpPoolManagement>>();
        ListResponse<IpPoolManagement> listResponse = new ListResponse<IpPoolManagement>();

        StringBuilder buildRequest =
            new StringBuilder(createVirtualDatacenterPublicPurchasedIPsLink(vdcId));
        buildRequest.append("?startwith=" + offset);
        buildRequest.append("&limit=" + numberOfNodes);
        buildRequest.append("&by=" + transformOrderBy(orderBy));
        buildRequest.append("&asc=" + (asc ? "true" : "false"));
        buildRequest.append("&onlyavailable=" + (onlyAvailable ? "true" : "false"));

        if (!filterLike.isEmpty())
        {
            buildRequest.append("&has=" + filterLike);
        }

        ClientResponse response = get(buildRequest.toString());

        if (response.getStatusCode() == 200)
        {
            IpsPoolManagementDto ips = response.getEntity(IpsPoolManagementDto.class);
            List<IpPoolManagement> flexIps = new ArrayList<IpPoolManagement>();

            for (IpPoolManagementDto ip : ips.getCollection())
            {
                IpPoolManagement flexIp = createFlexObject(ip);
                flexIps.add(flexIp);
            }
            listResponse.setList(flexIps);
            listResponse.setTotalNumEntities(ips.getTotalSize());

            dataResult.setData(listResponse);
            dataResult.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, dataResult, "getListNetworkPublicPoolByDatacenter");
        }

        return dataResult;
    }

    @Override
    public BasicResult getListNetworkPublicPoolToPurchaseByVirtualDatacenter(final Integer vdcId,
        final Integer offset, final Integer numberOfNodes, final String filterLike,
        final String orderBy, final Boolean asc) throws NetworkCommandException
    {
        DataResult<ListResponse<IpPoolManagement>> dataResult =
            new DataResult<ListResponse<IpPoolManagement>>();
        ListResponse<IpPoolManagement> listResponse = new ListResponse<IpPoolManagement>();

        StringBuilder buildRequest =
            new StringBuilder(createVirtualDatacenterPublicToPurchaseIPsLink(vdcId));
        buildRequest.append("?startwith=" + offset);
        buildRequest.append("&limit=" + numberOfNodes);
        buildRequest.append("&by=" + transformOrderBy(orderBy));
        buildRequest.append("&asc=" + (asc ? "true" : "false"));
        if (!filterLike.isEmpty())
        {
            buildRequest.append("&has=" + filterLike);
        }

        ClientResponse response = get(buildRequest.toString());

        if (response.getStatusCode() == 200)
        {
            IpsPoolManagementDto ips = response.getEntity(IpsPoolManagementDto.class);
            List<IpPoolManagement> flexIps = new ArrayList<IpPoolManagement>();

            for (IpPoolManagementDto ip : ips.getCollection())
            {
                IpPoolManagement flexIp = createFlexObject(ip);
                flexIps.add(flexIp);
            }
            listResponse.setList(flexIps);
            listResponse.setTotalNumEntities(ips.getTotalSize());

            dataResult.setData(listResponse);
            dataResult.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, dataResult, "getListNetworkPublicPoolByDatacenter");
        }

        return dataResult;
    }

    @Override
    public BasicResult getPrivateNetworks(final Integer vdcId)
    {
        DataResult<List<VlanNetwork>> result = new DataResult<List<VlanNetwork>>();

        String uri = createPrivateNetworksLink(vdcId);
        ClientResponse response = get(uri);

        if (response.getStatusCode() == 200)
        {
            VLANNetworksDto networksDto = response.getEntity(VLANNetworksDto.class);
            List<VlanNetwork> nets = new ArrayList<VlanNetwork>();

            for (VLANNetworkDto dto : networksDto.getCollection())
            {
                nets.add(createFlexObject(dto));
            }

            result.setData(nets);
            result.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, result, "getPrivateNetworks");
        }

        return result;
    }

    @Override
    public BasicResult getPublicNetwork(final Integer datacenterId, final Integer vlanId)
    {
        DataResult<VlanNetwork> result = new DataResult<VlanNetwork>();

        String uri = createPublicNetworkLink(datacenterId, vlanId);
        ClientResponse response = get(uri);

        if (response.getStatusCode() == 200)
        {
            VLANNetworkDto networkDto = response.getEntity(VLANNetworkDto.class);
            result.setData(createFlexObject(networkDto));
            result.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, result, "getPublicNetwork");
        }

        return result;

    }

    @Override
    public BasicResult purchasePublicIp(final Integer vdcId, final Integer ipId)
    {
        BasicResult result = new BasicResult();

        StringBuilder uri =
            new StringBuilder(createVirtualDatacenterPublicPurchasedIPLink(vdcId, ipId));

        ClientResponse response = put(uri.toString());

        if (response.getStatusCode() == 200)
        {
            result.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, result, "purchasePublicIp");
        }

        return result;
    }

    @Override
    public BasicResult releasePublicIp(final Integer vdcId, final Integer ipId)
    {
        BasicResult result = new BasicResult();

        StringBuilder uri =
            new StringBuilder(createVirtualDatacenterPublicToPurchaseIPLink(vdcId, ipId));

        ClientResponse response = put(uri.toString());

        if (response.getStatusCode() == 200)
        {
            result.setSuccess(Boolean.TRUE);
        }
        else
        {
            populateErrors(response, result, "releasePublicIp");
        }

        return result;
    }

    @Override
    public BasicResult setGatewayForVirtualMachine(final Integer vdcId, final Integer vappId,
        final Integer vmId, final IPAddress gateway)
    {
        BasicResult result = new BasicResult();
        String uri = createVirtualMachineConfigurationsLink(vdcId, vappId, vmId);

        ClientResponse response = get(uri);
        if (response.getStatusCode() == 200)
        {
            VMNetworkConfigurationsDto dtos = response.getEntity(VMNetworkConfigurationsDto.class);

            // Due we receive a gateway from the Flex client and we want to send an id
            // to update the gateway, search for the VMNetworkconfigurationDto object.
            for (VMNetworkConfigurationDto dto : dtos.getCollection())
            {
                if (dto.getGateway().equalsIgnoreCase(gateway.toString()))
                {
                    // Here we have found the dto. Modify it to inform we want to use this
                    // configuration
                    // by default.
                    String uriConfig =
                        createVirtualMachineConfigurationLink(vdcId, vappId, vmId, dto.getId());
                    dto.setUsed(Boolean.TRUE);

                    response = put(uriConfig, dto);
                    if (response.getStatusCode() == 200)
                    {
                        result.setSuccess(Boolean.TRUE);
                    }
                    else
                    {
                        populateErrors(response, result, "setGatewayForVirtualMachine");
                    }
                    return result;
                }
            }

            // Unknown exception. At least one result should be returned before.
            result.setSuccess(Boolean.FALSE);
            result.setMessage("Unknown exception while setting the gateway of the virtual machine "
                + vmId);
        }
        else
        {
            populateErrors(response, result, "setGatewayForVirtualMachine");
        }
        return result;
    }

    private IpPoolManagementDto createDtoObject(final IpPoolManagement ip)
    {
        IpPoolManagementDto dto = new IpPoolManagementDto();
        dto.setId(ip.getIdManagement());
        dto.setIp(ip.getIp());
        dto.setMac(ip.getMac());
        dto.setName(ip.getName());
        dto.setNetworkName(ip.getVlanNetworkName());
        dto.setQuarantine(ip.getQuarantine());
        dto.setAvailable(ip.getAvailable());
        return dto;
    }

    private IpPoolManagement createFlexObject(final IpPoolManagementDto ip)
    {
        IpPoolManagement flexIp = new IpPoolManagement();

        flexIp.setIdManagement(ip.getId());
        flexIp.setIp(ip.getIp());
        flexIp.setMac(ip.getMac());
        flexIp.setQuarantine(ip.getQuarantine());
        flexIp.setName(ip.getName());
        flexIp.setAvailable(ip.getAvailable());

        for (RESTLink currentLink : ip.getLinks())
        {
            if (currentLink.getRel().equalsIgnoreCase("privatenetwork"))
            {
                flexIp.setVlanNetworkName(currentLink.getTitle());
                flexIp.setVlanNetworkId(Integer.valueOf(currentLink.getHref().substring(
                    currentLink.getHref().lastIndexOf("/") + 1)));
            }
            if (currentLink.getRel().equalsIgnoreCase("publicnetwork"))
            {
                flexIp.setVlanNetworkName(currentLink.getTitle());
                flexIp.setVlanNetworkId(Integer.valueOf(currentLink.getHref().substring(
                    currentLink.getHref().lastIndexOf("/") + 1)));
            }
            else if (currentLink.getRel().equalsIgnoreCase("virtualdatacenter"))
            {
                flexIp.setVirtualDatacenterName(currentLink.getTitle());
                flexIp.setVirtualDatacenterId(Integer.valueOf(currentLink.getHref().substring(
                    currentLink.getHref().lastIndexOf("/") + 1)));
            }
            else if (currentLink.getRel().equalsIgnoreCase("virtualappliance"))
            {
                flexIp.setVirtualApplianceName(currentLink.getTitle());
                flexIp.setVirtualApplianceId(Integer.valueOf(currentLink.getHref().substring(
                    currentLink.getHref().lastIndexOf("/") + 1)));
            }
            else if (currentLink.getRel().equalsIgnoreCase("virtualmachine"))
            {
                flexIp.setVirtualMachineName(currentLink.getTitle());
                flexIp.setVirtualMachineId(Integer.valueOf(currentLink.getHref().substring(
                    currentLink.getHref().lastIndexOf("/") + 1)));
            }
        }

        return flexIp;
    }

    private VlanNetwork createFlexObject(final VLANNetworkDto dto)
    {
        NetworkConfiguration netconf = new NetworkConfiguration();
        netconf.setGateway(dto.getGateway());
        netconf.setMask(dto.getMask());
        netconf.setNetworkAddress(dto.getAddress());
        netconf.setPrimaryDNS(dto.getPrimaryDNS());
        netconf.setSecondaryDNS(dto.getSecondaryDNS());
        netconf.setSufixDNS(dto.getSufixDNS());
        netconf.setFenceMode("bridge");
        netconf.setNetmask(IPNetworkRang.transformIntegerMaskToIPMask(dto.getMask()).toString());

        VlanNetwork newNet = new VlanNetwork();
        newNet.setConfiguration(netconf);
        newNet.setDefaultNetwork(dto.getDefaultNetwork());
        newNet.setNetworkName(dto.getName());
        newNet.setVlanNetworkId(dto.getId());
        newNet.setVlanTag(dto.getTag());
        newNet.setNetworkId(dto.getId());

        return newNet;
    }

    private String transformOrderBy(final String orderBy)
    {
        if (orderBy == null)
        {
            return "ip";
        }
        else if (orderBy.equalsIgnoreCase("vlannetworkname"))
        {
            return "vlan";
        }
        else if (orderBy.equalsIgnoreCase("virtualappliancename"))
        {
            return "virtualappliance";
        }
        else if (orderBy.equalsIgnoreCase("virtualmachinename"))
        {
            return "virtualmachine";
        }
        else
        {
            return orderBy;
        }
    }

}
