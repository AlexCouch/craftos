package os

import net.minecraft.nbt.NBTTagCompound
import os.components.OSComponent
import os.components.OSLayout
import programs.Program
import system.DeviceSystem
import terminal.TerminalCommand
import terminal.TerminalStream

interface OperatingSystem{
    val name: String
    val apps: Set<Program>
    val commands: Set<TerminalCommand>
    val environment: OSInterface?

    fun start(system: DeviceSystem)
    fun serializeOS(): NBTTagCompound
    fun deserializeOS(nbt: NBTTagCompound)
}

interface OSInterface{
    val components: ArrayList<OSComponent>
    val os: OperatingSystem
    fun render(os: OperatingSystem, layout: OSLayout)
}

