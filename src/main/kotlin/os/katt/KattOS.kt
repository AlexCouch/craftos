package os.katt

import blocks.TileEntityDesktopComputer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import os.OSInterface
import os.OperatingSystem
import programs.Program
import system.DeviceSystem
import terminal.Terminal
import terminal.TerminalCommand
import terminal.TerminalStream

class KattOS : OperatingSystem {
    override val name: String
        get() = "katt"
    override val apps: Set<Program>
        get() = HashSet()
    override val commands: Set<TerminalCommand>
        get() = HashSet()
    override val terminal: Terminal
        get() = Terminal(this, TerminalStream())
    override val environment: OSInterface?
        get() = null

    override fun start(system: DeviceSystem) {
        environment?.components?.forEach {
            it.init()
        }
    }

    override fun serializeOS(): NBTTagCompound{
        val ret = NBTTagCompound()
        val appsnbt = NBTTagList()
        apps.forEach {
            appsnbt.appendTag(it.serialize())
        }
        ret.setTag("apps", appsnbt)
        val commandsnbt = NBTTagList()
        commands.forEach {
            commandsnbt.appendTag(it.serialize())
        }
        ret.setTag("commands", commandsnbt)
//        ret.setTag("terminal", terminal)
        return ret
    }

    override fun deserializeOS(nbt: NBTTagCompound) {
    }


    fun start(te: TileEntityDesktopComputer){

    }
}