package os.couch

import net.minecraft.nbt.NBTTagCompound
import network.NetworkPort
import network.Port
import os.OSInterface
import os.OperatingSystem
import programs.Program
import system.CouchDesktopSystem
import terminal.Terminal
import pkg.Package

class CouchOS(val system: CouchDesktopSystem, override val terminal: Terminal) : OperatingSystem {
    override val packages: ArrayList<Package>
        get() = arrayListOf()

    override fun registerPackage(pack: Package) {
        packages + pack
    }

    override val ports: ArrayList<Port<*>>
        get() = arrayListOf(
                    NetworkPort(0, system),
                    NetworkPort(1, system),
                    NetworkPort(2, system),
                    NetworkPort(2, system)
            )
    override val name: String
        get() = "couch"
    override val apps: Set<Program>
        get() = HashSet()
    override val environment: OSInterface?
        get() = null

    override fun start() {
        val nbt = NBTTagCompound()
        nbt.setString("name", this.name)
        system.memory.allocate(nbt)
    }

    override fun serializeOS(): NBTTagCompound = NBTTagCompound()
    override fun deserializeOS(nbt: NBTTagCompound) {}
}