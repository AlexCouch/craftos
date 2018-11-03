package os.couch

import client.SystemScreen
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants.NBT.TAG_STRING
import network.Port
import os.OperatingSystem
import os.filesystem.FileSystem
import os.filesystem.Folder
import stream
import system.CouchDesktopSystem
import terminal.*
import messages.*
import utils.printstr

class CouchOS(override val system: CouchDesktopSystem) : OperatingSystem {
    override val terminal: Terminal = CouchTerminal(this)
    override val fileSystem: FileSystem = FileSystem(this)

    override val ports: ArrayList<Port<*>>
        get() = arrayListOf()

    override val name: String
        get() = "CouchOS"
    override val screen: SystemScreen? by lazy {
        val cs = Minecraft.getMinecraft().currentScreen ?: return@lazy null
        if(cs is SystemScreen){
            return@lazy cs as? SystemScreen
        }
        return@lazy null
    }

    private fun printToBootScreen(message: String){
        println(message)
        stream.sendTo(PrintToBootScreenMessage(message, system.desktop.pos), system.player as EntityPlayerMP)
    }

    override fun start() {
        printToBootScreen("Starting up operating system...")
        val nbt = NBTTagCompound()
        nbt.setString("name", this.name)
        system.memory.allocate(nbt)
        printToBootScreen("\tMemory setup complete!")
        printToBootScreen("\tSetting up file system...")
        val homefolder = Folder("home", NBTTagCompound())
        val packagesfolder = Folder("packages", NBTTagCompound())
        homefolder.addFile(packagesfolder)
        this.fileSystem.currentDirectory.addFile(homefolder)
        printToBootScreen("Files in current directory...")
        this.fileSystem.currentDirectory.files.forEach {
            printToBootScreen("\t${it.name}")
        }
        printToBootScreen("\tFile system setup complete!")
        printToBootScreen("")
        printToBootScreen("Operating system ready...press enter to continue...")
        stream.sendTo(UnlockBootScreenInputMessage(this.system.desktop.pos), this.system.player as EntityPlayerMP)
    }

    override fun serializeOS(): NBTTagCompound{
        val tag = NBTTagCompound()
        val packsList = NBTTagList()
        val packs = this.terminal.packageManager.installedPackages
        packs.stream().forEach {
            packsList.appendTag(NBTTagString(it.name))
        }
        tag.setTag("packages", packsList)
        tag.setTag("filesystem", this.fileSystem.serialize())
        return tag
    }
    override fun deserializeOS(nbt: NBTTagCompound) {
        if(
                nbt.hasKey("packages") &&
                nbt.hasKey("filesystem")
        ){
            val packsList = nbt.getTagList("packages", TAG_STRING)
            packsList.forEach {
                this.terminal.packageManager.installPackage((it as NBTTagString).string)
            }
            val fileSystemTag = nbt.getCompoundTag("filesystem")
            this.fileSystem.deserialize(fileSystemTag)
        }
    }
}