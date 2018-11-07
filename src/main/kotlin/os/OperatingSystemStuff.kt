package os

import client.AbstractSystemScreen
import net.minecraft.nbt.NBTTagCompound
import network.Port
import os.filesystem.FileSystem
import system.DeviceSystem
import shell.Shell

interface OperatingSystem{
    val name: String
    val fileSystem: FileSystem
    val shell: Shell
    val ports: ArrayList<Port<*>>
    val screenAbstract: AbstractSystemScreen?
    val system: DeviceSystem<*>

    fun start()
    fun serializeOS(): NBTTagCompound
    fun deserializeOS(nbt: NBTTagCompound)

    /*fun getNextAvailablePort(): Port<*>?{
        for(port in this.ports){
            if(port.available){
                return port
            }
        }
        printstr("There are no ports available!", this.shell)
        return null
    }*/

    /*fun getNextAvailablePort(including: Class<*>): Port<*>?{
        for(port in this.ports){
            if(port.available){
                if(port::class.java == including){
                    return port
                }
            }
        }
        printstr("No available port of type: ${including.canonicalName}")
        return null
    }*/
}