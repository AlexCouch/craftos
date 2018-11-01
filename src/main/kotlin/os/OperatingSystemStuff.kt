package os

import net.minecraft.nbt.NBTTagCompound
import network.Port
import os.components.OSComponent
import os.components.OSLayout
import programs.Program
import terminal.Terminal
import pkg.Package

interface OperatingSystem{
    val name: String
    val apps: Set<Program>
    val terminal: Terminal
    val environment: OSInterface?
    val ports: ArrayList<Port<*>>
    val packages: ArrayList<Package>

    fun start()
    fun registerPackage(pack: pkg.Package)
    fun serializeOS(): NBTTagCompound
    fun deserializeOS(nbt: NBTTagCompound)
    /**
     * Will mostly be used to check if ports are functioning correctly; mostly internal
     */
    fun getNextAvailablePort(): Port<*>{
        for(port in this.ports){
            if(port.available){
                return port
            }
        }
        throw RuntimeException(IllegalStateException("There are no ports available!"))
    }

    /**
     * If you need a specific kind of port (video, sound, network, input, etc)
     *
     * This is what you want most of the time
     */
    fun getNextAvailablePort(including: Class<*>): Port<*>{
        for(port in this.ports){
            if(port.available){
                if(port::class.java == including){
                    return port
                }
            }
        }
        throw RuntimeException(IllegalStateException("No available port of type: ${including.canonicalName}"))
    }
}

interface OSInterface{
    val components: ArrayList<OSComponent>
    val os: OperatingSystem
    fun render(os: OperatingSystem, layout: OSLayout)
}

