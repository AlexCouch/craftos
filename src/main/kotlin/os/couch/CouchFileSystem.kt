package os.couch

import blocks.TileEntityDesktopComputer
import messages.MessageFactory
import messages.ProcessData
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import os.OperatingSystem
import os.filesystem.FileSystem
import os.filesystem.Folder
import os.filesystem.File
import os.filesystem.getFileTypeByName
import system.CouchDesktopSystem
import utils.getCurrentComputer
import kotlin.streams.toList

class CouchFileSystem(private val os: OperatingSystem) : FileSystem {

    private var root = Folder("root", NBTTagCompound())
    override var currentDirectory = root

    private val system = this.os.system as CouchDesktopSystem

    override fun changeDirectory(path: String): Boolean {
        if(path == ".."){
            if(currentDirectory.parent == null) return false
            currentDirectory = currentDirectory.parent ?: return false
            syncWithClient()
            return true
        }else if(path == root.path) {
            this.currentDirectory = root
            syncWithClient()
            return true
        }
        var currDir = this.currentDirectory
        val pathsplit = path.split('/')
        val filter = pathsplit.stream().filter { it.isNotBlank() }
        val it = filter.iterator()
        for(p in it){
            if(currDir.files.stream().anyMatch { it.name == p }){
                val new = currDir.files.stream().filter { it.name == p }.findFirst().get()
                if(new is Folder){
                    currDir = new
                    continue
                }
                os.shell.printStringServer("$p is not a directory; cd cancelled!", system.desktop.pos, system.player as EntityPlayerMP)
                return false
            }
        }
        this.currentDirectory = currDir
        syncWithClient()
        return true
    }

    override fun syncWithClient() {
        val prepareData = {
            val cddata = this.currentDirectory.data
            val nbt = NBTTagCompound()
            nbt.setString("name", this.currentDirectory.name)
            nbt.setTag("data", cddata)
            nbt
        }
        val processData: ProcessData = { data, world, pos, player ->
            val comp = getCurrentComputer(world, pos, player)!!
            val system = comp.system
            val os = system.os!!
            val fname = data.getString("name")
            val fdata = data.getCompoundTag("data")
            val currdir = Folder(fname, fdata)
            os.fileSystem.currentDirectory = currdir
        }
        val player = (this.os.system.te as TileEntityDesktopComputer).player as EntityPlayerMP
        MessageFactory.sendDataToClient("syncFSClient", player, this.os.system.te.pos, prepareData, processData)
    }

    override fun makeDirectory(path: String, callback: (() -> NBTTagCompound)?): Boolean {
        if(this.doesFileExist(path)){
            return false
        }
        val pathsplit = path.split('/')
        val currDir = this.currentDirectory
        val parentpath = StringBuilder()
        val sublist = pathsplit.subList(0, pathsplit.size-1)
        sublist.forEach {
            parentpath.append(it)
            parentpath.append("/")
        }
        if(this.changeDirectory(parentpath.toString())){
            this.currentDirectory.addFile(Folder(pathsplit[pathsplit.size - 1], callback?.invoke()
                    ?: NBTTagCompound()))
            this.changeDirectory(currDir.path)
            return true
        }
        return false
    }

    override fun deleteDirectory(path: String): Boolean {
        return this.deleteFile(path)
    }

    override fun makeFile(path: String, fileType: String, here: Boolean, callback: (() -> NBTTagCompound)?): Boolean {
        if(this.doesFileExist(path, here)){
            return false
        }
        if(here){
            val file = File(path, getFileTypeByName(fileType), callback?.invoke() ?: NBTTagCompound())
            this.currentDirectory + file
        }else{
            val pathsplit = path.split('/')
            val currDir = this.currentDirectory
            val parentpath = StringBuilder()
            pathsplit.subList(0, pathsplit.size-2).forEach {
                parentpath.append(it)
                parentpath.append("/")
            }
            if(this.changeDirectory(parentpath.toString())){
                this.currentDirectory.addFile(File(pathsplit[pathsplit.size - 1], getFileTypeByName(fileType), callback?.invoke() ?: NBTTagCompound()))
                this.currentDirectory = currDir
                return true
            }
        }
        return false
    }

    override fun deleteFile(path: String, here: Boolean): Boolean  {
        val file = this.getFile(path, here) ?: return false
        val parent = file.parent ?: return false
        parent - file

        return false
    }

    override fun getFile(path: String, here: Boolean): File<*>? = this.checkAndGetFile(path, here).second

    override fun writeToFileWithPath(path: String, callback: () -> NBTTagCompound): Boolean {
        val file = this.getFile(path, false) ?: return false
        file.data = callback()

        return true
    }

    override fun doesFileExist(path: String, here: Boolean): Boolean = this.checkAndGetFile(path, here).first

    private fun checkAndGetFile(path: String, here: Boolean = false): Pair<Boolean, File<*>?>{
        var exists = false
        var file: File<*>? = null
        if(here){
            exists = this.currentDirectory.files.stream().anyMatch { it.name == path }
        }else{
            val pathsplit = path.split('/').stream().filter { it.isNotBlank() }.toList()
            var parent = root
            var i = 0
            while(true){
                val f = pathsplit[i]
                if(parent.files.stream().anyMatch { it.name == f }){
                    val f1 = parent.files.stream().filter { it.name == f }.findFirst().get()
                    if(f1 is Folder){
                        parent = f1
                        i++
                    }else{
                        if(i == pathsplit.size - 1){
                            file = f1
                            exists = true
                        }
                        break
                    }
                }else{
                    break
                }
            }
        }
        return Pair(exists, file)
    }


    override fun serialize(): NBTTagCompound = root.data

    override fun deserialize(nbt: NBTTagCompound){
        root = Folder("root", nbt)
    }

}