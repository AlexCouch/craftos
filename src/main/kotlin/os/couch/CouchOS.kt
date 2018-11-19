package os.couch

import client.BootScreen
import client.AbstractSystemScreen
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
import system.CouchDesktopSystem
import shell.*
import messages.*
import pkg.texteditor.TextEditorPackage
import java.util.*

class CouchOS(override val system: CouchDesktopSystem) : OperatingSystem {
    override val shell: Shell = CouchShell(this)
    override val fileSystem: FileSystem = FileSystem(this)

    override val ports: ArrayList<Port<*>>
        get() = arrayListOf()

    override val name: String
        get() = "CouchOS"
    override val screenAbstract: AbstractSystemScreen? by lazy {
        val cs = Minecraft.getMinecraft().currentScreen ?: return@lazy null
        if(cs is AbstractSystemScreen){
            return@lazy cs as? AbstractSystemScreen
        }
        return@lazy null
    }

    private fun printToBootScreen(message: String){
        val random = Random()
        println(message)
        val prepareData = {
            val nbt = NBTTagCompound()
            nbt.setString("message", message)
            nbt
        }
        val processData: ProcessData = { data, _, _, _ ->
            val str = data.getString("message")
            val cs = Minecraft.getMinecraft().currentScreen
            if(cs is BootScreen){
                cs.printToScreen(str)
            }
        }
        MessageFactory.sendDataToClient("printToBootScreen_${random.nextInt()}", system.player as EntityPlayerMP, this.system.desktop.pos, prepareData, processData)
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
        printToBootScreen("\tFile system setup complete!")
        printToBootScreen("")
        printToBootScreen("Operating system ready...press enter to continue...")
        unlockBootScreenInput()
    }

    private fun unlockBootScreenInput(){
        val prepareData = { NBTTagCompound() }
        val processData: ProcessData = { _, _, _, _ ->
            val currentScreen = Minecraft.getMinecraft().currentScreen
            if(currentScreen is BootScreen){
                currentScreen.allowInput = true
            }
        }
        MessageFactory.sendDataToClient("unlockBootScreenInput", this.system.player as EntityPlayerMP, this.system.desktop.pos, prepareData, processData)
    }

    override fun serializeOS(): NBTTagCompound{
        val tag = NBTTagCompound()
        val packsList = NBTTagList()
        val packs = this.shell.packageManager.installedPackages
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
                this.shell.packageManager.installPackage((it as NBTTagString).string)
            }
            val fileSystemTag = nbt.getCompoundTag("filesystem")
            this.fileSystem.deserialize(fileSystemTag)
        }
    }
}