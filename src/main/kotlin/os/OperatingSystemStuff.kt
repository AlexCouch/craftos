package os

import client.SystemScreen
import net.minecraft.nbt.NBTTagCompound
import network.Port
import os.filesystem.FileSystem
import programs.Program
import system.DeviceSystem
import terminal.Terminal
import utils.printstr

interface OperatingSystem{
    val name: String
    val apps: Set<Program>
    val fileSystem: FileSystem
    val terminal: Terminal
    val ports: ArrayList<Port<*>>
    val screen: SystemScreen?
    val system: DeviceSystem<*>

    fun start()
    fun serializeOS(): NBTTagCompound
    fun deserializeOS(nbt: NBTTagCompound)
    /**
     * Will mostly be used to check if ports are functioning correctly; mostly internal
     */
    fun getNextAvailablePort(): Port<*>?{
        for(port in this.ports){
            if(port.available){
                return port
            }
        }
        printstr("There are no ports available!", this.terminal)
        return null
    }

    /**
     * If you need a specific kind of port (video, sound, network, input, etc)
     *
     * This is what you want most of the time
     */
    fun getNextAvailablePort(including: Class<*>): Port<*>?{
        for(port in this.ports){
            if(port.available){
                if(port::class.java == including){
                    return port
                }
            }
        }
        printstr("No available port of type: ${including.canonicalName}")
        return null
    }
}