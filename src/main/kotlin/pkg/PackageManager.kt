package pkg

import client.AbstractSystemScreen
import client.GuiRegistry
import messages.ProcessData
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import shell.Shell
import system.CouchDesktopSystem
import messages.MessageFactory

abstract class Package(val system: CouchDesktopSystem){
    abstract val name: String
    abstract val version: String
    abstract fun init()
    abstract fun onUpdate()
}

abstract class RenderablePackage(system: CouchDesktopSystem) : Package(system){
    abstract val renderer: AbstractSystemScreen

    override fun init() {
        GuiRegistry.registerGui("${this.name}_renderer", this.renderer)

        val prepareData: () -> NBTTagCompound = {
            val nbt = NBTTagCompound()
            nbt.setString("pack_name", "${this.name}_renderer")
            nbt
        }
        val processData: ProcessData = { nbt, world, pos, player ->
            val name = nbt.getString("pack_name")
            GuiRegistry.openGui(name, player, world, pos)
        }
        MessageFactory.sendDataToClient(system.player as EntityPlayerMP, this.system.desktop.pos, prepareData, processData)
    }
}

class PackageManager(val shell: Shell){
    private val availablePackages = hashSetOf<Package>()
    val installedPackages = hashSetOf<Package>()

    private val system = shell.os.system as CouchDesktopSystem

    //I don't want anybody being able to do any operations on the available packages.
    fun registerPackage(pack: Package){
        this.availablePackages.add(pack)
    }

    fun isPackageInstalled(name: String) = installedPackages.stream().anyMatch { it.name == name }
    fun isPackageAvailable(name: String) = availablePackages.stream().anyMatch { it.name == name }
    fun getAvailablePackage(name: String): Package?{
        if(isPackageAvailable(name)){
            val filteredPack = availablePackages.stream().filter { it.name == name }.findFirst()
            if(filteredPack.isPresent){
                return filteredPack.get()
            }
        }
        this.shell.printStringServer("Could not find available package of name '$name'.", this.system.player as EntityPlayerMP)
        return null
    }

    fun getInstalledPackage(name: String): Package?{
        if(isPackageInstalled(name)){
            val filteredPack = installedPackages.stream().filter { it.name == name }.findFirst()
            if(filteredPack.isPresent){
                return filteredPack.get()
            }
        }
        this.shell.printStringServer("Could not find installed package of name '$name'.", system.player as EntityPlayerMP)
        return null
    }

    fun installPackage(packname: String){
        if(isPackageAvailable(packname)){
            val pack = getAvailablePackage(packname) ?: return
            if(isPackageInstalled(packname)){
                this.shell.printStringServer("That package is already installed: $packname", this.system.desktop.player as EntityPlayerMP)
                return
            }
            this.installedPackages += pack
            this.shell.printStringServer("Package $packname has been successfully installed!", system.player as EntityPlayerMP)
            return
        }
        this.shell.printStringServer("There is no package with name $packname.", system.player as EntityPlayerMP)
    }

    fun uninstallPackage(name: String){
        if(isPackageInstalled(name)){
            val installedPackage = getInstalledPackage(name) ?: return
            this.installedPackages.remove(installedPackage)
            this.shell.printStringServer("Package '$name' has been uninstalled!", system.player as EntityPlayerMP)
        }
        this.shell.printStringServer("There is no package installed with name '$name'.", system.player as EntityPlayerMP)
    }
}
