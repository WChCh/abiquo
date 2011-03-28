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

package com.abiquo.tarantino.plugins.esxi.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.abiquo.commons.amqp.impl.datacenter.domain.jobs.DiskStandard;
import com.abiquo.commons.amqp.impl.datacenter.domain.jobs.SnapshootVirtualMachine;
import com.abiquo.commons.amqp.impl.datacenter.domain.jobs.VirtualMachineDefinitionDto.PrimaryDisk.DiskStandardConf;
import com.abiquo.tarantino.errors.VirtualFactoryErrors;
import com.abiquo.tarantino.errors.VirtualFactoryException;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualSCSIController;

/**
 * All the related code to Disk attachement <br>
 * working references <br>
 * deviceName = "/vmfs/devices/disks/vmhba34:5:0:0" lunUuid =
 * "0200000000600144f04a324a7900000c296262e000534f4c415249" iSCSI references:
 * https://www.vmware.com/communities/content/developer/samplecode/java/CreateVmfsDatastore.html
 * http://blog.laspina.ca/ubiquitous/tag/iscsi
 * http://www.vmadmin.co.uk/resources/35-esxserver/58-rdmvm google
 * "config.storageDevice.softwareInternetScsiEnabled"
 * http://209.85.229.132/search?q=cache:bBrXSZnbOOgJ
 * :communities.vmware.com/servlet/JiveServlet/previewBody
 * /9606-102-1-6951/AddRemoveVirtualDisk.java.doc%3Bjsessionid%3D81671F
 * 936ECC6473E367D9B56B999586+ArrayOfHostHostBusAdapter&cd=1&hl=es&ct=clnk&gl=es&client=firefox-a
 */
public class VmwareMachineDisk
{
    /** Constant logger object. */
    private final static Logger logger = LoggerFactory.getLogger(VmwareMachineDisk.class);

    /** Utilities to manage the main SDK API. */
    private EsxiUtils utils;

    public VmwareMachineDisk(EsxiUtils utils)
    {
        this.utils = utils;
    }

    /**
     * Gets the virtual disk path form the codified "[repository]diskPath" from the source datastore
     * (SAN) on configuration (on the OVF disk location)
     */
    private String getSourceDiskPath(DiskStandardConf diskConf)
    {
        final DiskStandard disk = diskConf.getDiskStandard();

        // TODO DATASTORE -- obtain the [datastorename] based on the datastore location

        return String.format("[%s] %s", disk.getDatastore(), disk.getPath());
    }

    /**
     * Codify the disk destination path on the target datastore (VMFS).
     */
    private String getDestinationDiskPath(DiskStandardConf diskConf, final String machineUuid)
    {

        // TODO DATASTORE -- obtain the [datastorename] based on the datastore location

        return String.format("[%s] %s/%s-flat.vmdk", diskConf.getDestinationDatastore(),
            machineUuid, machineUuid);
    }

    /**
     * Private helper to clone a virtual disk from the repository to the datastore. (RImp
     * substitution)
     * 
     * @throws Exception
     */
    public void moveVirtualDiskToDataStore(DiskStandardConf diskConf, final String machineUuid)
        throws VirtualFactoryException
    {

        ManagedObjectReference dcmor;
        ManagedObjectReference fileManager;
        String sourcePath;
        String destPath;
        ManagedObjectReference taskCopyMor;

        dcmor = utils.getDatacenterMor();

        fileManager = utils.getServiceInstance().getServiceContent().getFileManager();

        sourcePath = getSourceDiskPath(diskConf);
        destPath = getDestinationDiskPath(diskConf, machineUuid);

        logger.info(
            "Moving image from source repository path [{}] into destination datastore [{}] ",
            sourcePath, destPath);

        try
        {
            taskCopyMor =
                utils.getVimStub().copyDatastoreFile_Task(fileManager, sourcePath, dcmor, destPath,
                    dcmor, true);
        }
        catch (Exception e)
        {
            final String detail =
                String.format(
                    "Virtual Machine %s using source disk at : %s and destintatino %s\nCaused By:",
                    machineUuid, sourcePath, destPath, e.toString());

            throw new VirtualFactoryException(VirtualFactoryErrors.CLONING_DISK, detail);
        }

        utils.checkTaskState(taskCopyMor);

        logger.info("Virtual Machine [{}] cloned Sucessfully", machineUuid);
    }

 
    /**
     * Gets the destination path of the bundle virtual disk
     * 
     * @param destinationDatastoreName
     * @param snapShotName
     * @return
     */
    private String getDestinationPathToBundle(DiskStandard destination)
    // (String destinationDatastoreName,
    // String destinationPath, String snapShotName)
    {

        //
        //
        // String finalPath = destinationPath;
        //
        // if (finalPath == null)
        // {
        // String virtualDiskPath = vmConfig.getVirtualDiskBase().getLocation();
        //
        // int pos = virtualDiskPath.lastIndexOf("]");
        // String fileName = virtualDiskPath.substring(pos + 1);
        //
        // int pos2 = fileName.lastIndexOf(File.separatorChar);
        // finalPath = fileName.substring(0, pos2);
        // }
        //
        // String destinationFile =
        // "[" + destinationDatastoreName + "] " + finalPath + "/" + snapShotName;
        //
        //
        // return destinationFile;

        // TODO DATASTORE -- obtain the [datastorename] based on the datastore location
        return String.format("[%s] %s", destination.getDatastore(), destination.getPath());
    }

    /**
     * Gets the source disk path of the virtual disk to bundle
     * 
     * @param sourceDatastoreName
     * @param sourcePath the source path
     * @param isManaged
     * @return
     */
    private String getSourceDiskPathToBundle(DiskStandard disk)

    // (String sourceDatastoreName, String sourcePath,
    // boolean isManaged)
    {
        //
        //
        // if (isManaged)
        // {
        // return "[" + sourceDatastoreName + "] " + machineName + "/" + machineName
        // + "-flat.vmdk";
        // }
        // else
        // {
        // return "[" + sourceDatastoreName + "] " + sourcePath;
        // }

        // TODO DATASTORE -- obtain the [datastorename] based on the datastore location
        return String.format("[%s] %s", disk.getDatastore(), disk.getPath());
    }

    /**
     * Bundles a virtual disk from the local Repository
     * 
     * @param sourcePath TODO
     * @param isManaged
     * @throws VirtualMachineException
     */
    // public void bundleVirtualDisk(String sourcePath, String destinationPath, String snapShotName,
    // boolean isManaged) throws VirtualMachineException
    // {
    // bundleVirtualDisk(sourcePath, vmConfig.getVirtualDiskBase().getTargetDatastore(),
    // vmwareConfig.getDatastoreSanName(), destinationPath, snapShotName, isManaged);
    // }

    /**
     * Private helper to bundle the virtual disk
     * 
     * @param sourcePath TODO
     * @param sourceDatastoreName
     * @param destinationDatastoreName
     * @param snapShotName
     * @param isManaged
     * @throws VirtualMachineException
     */
    public void bundleVirtualDisk(SnapshootVirtualMachine snpahot) throws VirtualFactoryException
    // (String sourcePath, String sourceDatastoreName,
    // String destinationDatastoreName, String destinationPath, String snapShotName,
    //
    // boolean isManaged) throws VirtualFactoryException
    {
        ManagedObjectReference dcmor;
        ManagedObjectReference fileManager;
        String sourcePathComposed;
        String destPath;
        ManagedObjectReference taskCopyMor;

        dcmor = utils.getDatacenterMor();

        fileManager = utils.getServiceInstance().getServiceContent().getFileManager();

        // TODO not for statefull images !!!

        sourcePathComposed = getSourceDiskPathToBundle(snpahot.getSourceDisk().getDiskStandard());// (sourceDatastoreName,
                                                                                                  // sourcePath,
                                                                                                  // isManaged);

        destPath = getDestinationPathToBundle(snpahot.getDestinationDisk());// (destinationDatastoreName,
                                                                            // destinationPath,
                                                                            // snapShotName);

        logger.info(
            "Moving image from source repository path [{}] into destination datastore [{}] ",
            sourcePathComposed, destPath);
        try
        {
            taskCopyMor =
                utils.getVimStub().copyDatastoreFile_Task(fileManager, sourcePathComposed, dcmor,
                    destPath, dcmor, true);
        }
        catch (Exception e)
        {
            final String detail = String.format("from %s to %s", sourcePathComposed, destPath);
            throw new VirtualFactoryException(VirtualFactoryErrors.SNAPSHOT, detail);
        }

        utils.checkTaskState(taskCopyMor);

        logger.info("Virtual Machine cloned Sucessfully");
    }

    
    /**
     * Configure extended virtual disk on during VM creation.
     */
    // public VirtualDeviceConfigSpec[] initialDiskDeviceConfigSpec() throws Exception
    // {
    // ArrayList<VirtualDeviceConfigSpec> deviceConfig = new ArrayList<VirtualDeviceConfigSpec>();
    //
    // List<VirtualDisk> extendedDiskList = vmConfig.getExtendedVirtualDiskList();
    //
    // // added disks
    // for (VirtualDisk vDisk : extendedDiskList)
    // {
    // // adds the new disk
    // deviceConfig.add(addVirtualDiskFromConfiguration(vDisk));
    // // TODO newVirtualDisk.setLocation(location);
    // }
    //
    // return deviceConfig.toArray(new VirtualDeviceConfigSpec[] {});
    // }

    /**
     * Only during reconfiguration, check which disk change its configuration, or new added/removed.
     * 
     * @param newConfiguration the new virtual disk requirements.
     * @return a device configuration to add/remove the provided disk requirements.
     * @throws Exception
     */
    // public VirtualDeviceConfigSpec[] getDiskDeviceConfigSpec(
    // VirtualMachineConfiguration newConfiguration) throws Exception
    // {
    // ArrayList<VirtualDeviceConfigSpec> deviceConfig = new ArrayList<VirtualDeviceConfigSpec>();
    //
    // // Compares the two lists and adds or remove the disks
    // List<VirtualDisk> newExtendedDiskList = newConfiguration.getExtendedVirtualDiskList();
    // List<VirtualDisk> oldExtendedDiskList = vmConfig.getExtendedVirtualDiskList();
    //
    // /*
    // * // If there are no more extended disks, I remove the existent ones if
    // * ((newExtendedDiskList.size() == 0) && (oldExtendedDiskList.size() == 0)) { return null; }
    // */
    //
    // // all the previously added disk id.
    // Map<String, VirtualDisk> htOldDiskId = new Hashtable<String, VirtualDisk>();
    // for (VirtualDisk ovd : oldExtendedDiskList)
    // {
    // logger.debug("old disk [{}]", ovd.getId());
    // htOldDiskId.put(ovd.getId(), ovd);
    // }
    //
    // // all the new added disk id.
    // Map<String, VirtualDisk> htNewDiskId = new Hashtable<String, VirtualDisk>();
    // for (VirtualDisk nvd : newExtendedDiskList)
    // {
    // logger.debug("new disk [{}]", nvd.getId());
    // htNewDiskId.put(nvd.getId(), nvd);
    // }
    //
    // // check removed disks
    // for (String oldId : htOldDiskId.keySet())
    // {
    // if (!htNewDiskId.keySet().contains(oldId))
    // {
    // // removes the disk
    // deviceConfig.add(removeVirtualDiskFromConfig(htOldDiskId.get(oldId)));
    // }
    // // TODO checking disk resize
    // }
    //
    // // check added disks
    // for (String newId : htNewDiskId.keySet())
    // {
    // if (!htOldDiskId.keySet().contains(newId))
    // {
    // // adds the new disk
    // deviceConfig.add(addVirtualDiskFromConfiguration(htNewDiskId.get(newId)));
    // // TODO newVirtualDisk.setLocation(location);
    // }
    //
    // }
    //
    // /*
    // * TODO resize else if ((newExtendedDiskList.get(0).getCapacity() >
    // * oldExtendedDiskList.get(0) .getCapacity())) { long diffSize =
    // * newExtendedDiskList.get(0).getCapacity(); String diskLocation =
    // * oldExtendedDiskList.get(0).getLocation();
    // * newExtendedDiskList.get(0).setLocation(diskLocation);
    // * logger.info("Extending to {} bytes in the disk location: {}", diffSize, diskLocation); //
    // * The new extended disk capacity is larger than the old one extendVirtualDisk(diskLocation,
    // * diffSize); return null; } else if ((newExtendedDiskList.get(0).getCapacity() <
    // * oldExtendedDiskList.get(0) .getCapacity())) { // EXPERIMENTAL ! long diffSize =
    // * newExtendedDiskList.get(0).getCapacity(); String diskLocation =
    // * oldExtendedDiskList.get(0).getLocation();
    // * newExtendedDiskList.get(0).setLocation(diskLocation);
    // * logger.info("Shrinking the disk location: {}", diffSize, diskLocation);
    // * shrinkVirtualDisk(diskLocation, diffSize); //
    // * reduceVirtualDisk(newExtendedDiskList.get(0),diskSpec); return null; } else if
    // * ((newExtendedDiskList.get(0).getCapacity() == oldExtendedDiskList.get(0) .getCapacity()))
    // * { return null; }
    // */
    //
    // // List<VirtualDisk> disksToAdd = new ArrayList<VirtualDisk>(newExtendedDiskList);
    // // List<VirtualDisk> disksToDelete = new ArrayList<VirtualDisk>(oldExtendedDiskList);
    // /*
    // * // Adding the new virtual disk lists if ((disksToAdd.removeAll(oldExtendedDiskList)) ||
    // * (oldExtendedDiskList.size() == 0)) { for (VirtualDisk newVirtualDisk : disksToAdd) {
    // * addVirtualDiskToConfig(newVirtualDisk, diskSpec); } } //Removing disks if
    // * (disksToDelete.removeAll(newExtendedDiskList)) { for (VirtualDisk oldVirtualDisk :
    // * disksToDelete) { removeVirtualDiskFromConfig(oldVirtualDisk, diskSpec); } }
    // */
    //
    // return deviceConfig.toArray(new VirtualDeviceConfigSpec[] {});
    // }

    /**
     * Create the representation to add the virtual disk on the device configuration.
     * 
     * @throws Exception
     */
    // public VirtualDeviceConfigSpec addVirtualDiskFromConfiguration(DiskStandard vd)
    // throws Exception
    // {
    // com.vmware.vim25.VirtualDisk virtualDisk;
    // VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();
    //
    // logger.debug("Adding disk Id[{}] location [{}] type[" + vd.getDiskType().name() + "]",
    // vd.getId(), vd.getLocation());
    //
    // if (vd.getDiskType() == VirtualDiskType.STANDARD)
    // {
    // virtualDisk = createStandardDisk(vd);
    // }
    // else
    // {
    // throw new VirtualMachineException("Invalid virtual disk type " + vd.getDiskType());
    // }
    //
    // diskSpec.setDevice(virtualDisk);
    //
    // diskSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
    // diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.create);
    //
    // return diskSpec;
    // }

    /**
     * Create the representation to delete the virtual disk on the device configuration.
     * 
     * @throws Exception
     */
    // public VirtualDeviceConfigSpec removeVirtualDiskFromConfig(VirtualDisk vd) throws Exception
    // {
    // com.vmware.vim25.VirtualDisk virtualDisk;
    // VirtualDeviceConfigSpec diskSpec = new VirtualDeviceConfigSpec();
    //
    // logger.debug("Remove disk Id[{}] location [{}] type [" + vd.getDiskType().name() + "]",
    // vd.getId(), vd.getLocation());
    //
    // if (vd.getDiskType() == VirtualDiskType.STANDARD)
    // {
    // logger.warn("Removind flat disk not activated");
    // return null;
    // }
    // return null;
    // }

    // /**
    // * @deprecated only iSCSI disk to be added. Adds the virtual Disk to the virtual device
    // * configuration
    // * @param newVirtualDisk the new virtual disk to add
    // * @param diskSpec the virtual disk device configuration
    // * @throws Exception
    // */
    // @Deprecated
    // public com.vmware.vim25.VirtualDisk createStandardDisk(VirtualDisk newVirtualDisk)
    // throws Exception
    // {
    // ManagedObjectReference _virtualMachine = utils.getVmMor(vmConfig.getMachineName());
    // VirtualMachineConfigInfo vmConfigInfo =
    // (VirtualMachineConfigInfo) utils.getServiceUtils().getDynamicProperty(_virtualMachine,
    // "config");
    // com.vmware.vim25.VirtualDisk disk = new com.vmware.vim25.VirtualDisk();
    // VirtualDiskFlatVer2BackingInfo diskfileBacking = new VirtualDiskFlatVer2BackingInfo();
    // String dsName = vmConfig.getVirtualDiskBase().getTargetDatastore();
    //
    // int unitNumber;
    // VirtualDevice[] devices = vmConfigInfo.getHardware().getDevice();
    // int ckey = getSCSIControllerKey();// getControllerKey("SCSI Controller 0");
    //
    // unitNumber = devices.length + 1;
    // String fileName =
    // "[" + dsName + "] " + machineName + "/" + newVirtualDisk.getId() + ".vmdk";
    //
    // diskfileBacking.setFileName(fileName);
    // // Provisioning with thin provisioning
    // diskfileBacking.setThinProvisioned(true);
    // diskfileBacking.setDiskMode(VirtualDiskMode.persistent.name());
    //
    // disk.setControllerKey(ckey);
    // disk.setUnitNumber(unitNumber);
    // disk.setBacking(diskfileBacking);
    //
    // Description des = new Description();
    // des.setLabel(newVirtualDisk.getId());
    // des.setSummary(newVirtualDisk.getId());
    // disk.setDeviceInfo(des);
    //
    // int size = (int) (newVirtualDisk.getCapacity() / 1024);
    // disk.setCapacityInKB(size);
    // disk.setKey(-1);
    //
    // /*
    // * //disk.setDynamicType(newVirtualDisk.getId()); DynamicProperty dp = new
    // * DynamicProperty(); dp.setName("UUID"); dp.setVal(newVirtualDisk.getId());
    // * disk.setDynamicProperty(new DynamicProperty[]{dp});
    // */
    //
    // newVirtualDisk.setLocation(fileName); // XXX
    //
    // return disk;
    // }

    /*
     * private int getSCSIControlerKey(VirtualDevice[] devices) { int ckey = 0; for (int k = 0; k <
     * devices.length; k++) { if
     * (devices[k].getDeviceInfo().getLabel().equalsIgnoreCase("SCSI Controller 0")) { ckey =
     * devices[k].getKey(); } } return ckey; }
     */

    protected int getSCSIControllerKey(final String machineUuid) // throws Exception
    {
        int ckey = 0;

        try
        {

            ManagedObjectReference virtualMachine = utils.getVmMor(machineUuid);
            VirtualMachineConfigInfo vmConfigInfo =

            (VirtualMachineConfigInfo) utils.getDynamicProperty(virtualMachine, "config");

            VirtualDevice[] devices = vmConfigInfo.getHardware().getDevice();

            for (VirtualDevice device : devices)
            {
                if (device instanceof VirtualSCSIController)
                {
                    ckey = device.getKey();
                }
            }

        }
        catch (Exception e)
        {
            // If is not already configured is being configured right now, we configure it on ''1''
            ckey = 1;
        }

        return ckey;
    }

    // /**
    // * @deprecated resize not considered Adds the virtual Disk to the virtual device configuration
    // * @param newVirtualDisk the new virtual disk to add
    // * @param diskSpec the virtual disk device configuration
    // * @throws Exception
    // */
    // @Deprecated
    // protected String reduceVirtualDisk(VirtualDisk newVirtualDisk, VirtualDeviceConfigSpec
    // diskSpec)
    // throws Exception
    // {
    // ManagedObjectReference _virtualMachine = utils.getVmMor(vmConfig.getMachineName());
    // VirtualMachineConfigInfo vmConfigInfo =
    // (VirtualMachineConfigInfo) utils.getServiceUtils().getDynamicProperty(_virtualMachine,
    // "config");
    // com.vmware.vim25.VirtualDisk disk = new com.vmware.vim25.VirtualDisk();
    // VirtualDiskFlatVer2BackingInfo diskfileBacking = new VirtualDiskFlatVer2BackingInfo();
    // String dsName = vmConfig.getVirtualDiskBase().getTargetDatastore();
    // int ckey = 0;
    // int unitNumber = 0;
    //
    // VirtualDevice[] test = vmConfigInfo.getHardware().getDevice();
    // for (int k = 0; k < test.length; k++)
    // {
    // if (test[k].getDeviceInfo().getLabel().equalsIgnoreCase("SCSI Controller 0"))
    // {
    // ckey = test[k].getKey();
    // }
    // }
    //
    // unitNumber = test.length + 1;
    // String fileName =
    // "[" + dsName + "] " + machineName + "/" + newVirtualDisk.getId() + ".vmdk";
    //
    // // virtualDiskId = newVirtualDisk.getId();
    //
    // diskfileBacking.setFileName(fileName);
    // // Provisioning with thin provisioning
    // diskfileBacking.setThinProvisioned(true);
    // diskfileBacking.setDiskMode("persistent");
    //
    // disk.setControllerKey(ckey);
    // disk.setUnitNumber(unitNumber);
    // disk.setBacking(diskfileBacking);
    // int size = (int) (newVirtualDisk.getCapacity() / 1024);
    // disk.setCapacityInKB(size);
    // disk.setKey(-1);
    //
    // diskSpec.setOperation(VirtualDeviceConfigSpecOperation.edit);
    // diskSpec.setDevice(disk);
    //
    // return fileName;
    //
    // }
    //
    // /**
    // * @deprecated resize not considered
    // */
    // @Deprecated
    // protected void shrinkVirtualDisk(String virtualDiskLocation, long diffSize) throws Exception
    // {
    // ManagedObjectReference virtualDiskManager =
    // utils.getServiceContent().getVirtualDiskManager();
    //
    // String dcName = utils.getOption("datacentername");
    // ManagedObjectReference dcmor =
    // utils.getServiceUtils().getDecendentMoRef(null, "Datacenter", dcName);
    //
    // if (dcmor == null)
    // {
    // String message = "Datacenter " + dcName + " not found.";
    // logger.error(message);
    // throw new VirtualMachineException(message);
    // }
    // // TODO Defragment the disk
    // ManagedObjectReference tmor =
    // utils.getService().shrinkVirtualDisk_Task(virtualDiskManager, virtualDiskLocation,
    // dcmor, false);
    // utils.monitorTask(tmor);
    //
    // }
    //

}
