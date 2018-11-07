package os.filesystem

import blocks.TileEntityDesktopComputer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import os.OperatingSystem
import messages.*
import os.couch.CouchOS
import system.CouchDesktopSystem
import utils.getCurrentComputer

class FileSystem(private val os: OperatingSystem){
    private var root = Folder("root", NBTTagCompound())
    var currentDirectory = root

    private val system = this.os.system as CouchDesktopSystem

    fun relocate(dirName: String): Boolean{
        if(dirName == ".."){
            currentDirectory = currentDirectory.parent ?: return false
            syncWithClient()
            return true
        }
        if(currentDirectory.files.stream().anyMatch { it.name == dirName }){
            val new = currentDirectory.files.stream().filter { it.name == dirName }.findFirst().get()
            if(new is Folder){
                currentDirectory = new
                syncWithClient()
                return true
            }
            os.shell.printStringServer("$dirName is not a directory; rel cancelled!", system.desktop.pos, system.player as EntityPlayerMP)
            return false
        }
        os.shell.printStringServer("$dirName does not exist in current directory!", system.desktop.pos, system.player as EntityPlayerMP)
        return false
    }

    private fun syncWithClient(){
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
        MessageFactory.sendDataToClient(player, this.os.system.te.pos, prepareData, processData)
    }

    fun makeDirectory(dirName: String, callback: (() -> NBTTagCompound)?): Boolean{
        if(currentDirectory.files.stream().anyMatch { it.name == dirName }){
            os.shell.printStringServer("File with name '$dirName' already exists.", system.desktop.pos, system.player as EntityPlayerMP)
            return false
        }
        val data = callback?.invoke() ?: NBTTagCompound()
        this.currentDirectory + Folder(dirName, data)
        return true
    }

    fun makeDirectory(dirName: String) = makeDirectory(dirName, null)

    fun makeFile(fileName: String, callback: (() -> NBTTagCompound)?): Boolean{
        if(currentDirectory.files.stream().anyMatch { it.name == fileName }){
            os.shell.printStringServer("File with name '$fileName' already exists.", system.desktop.pos, system.player as EntityPlayerMP)
            return false
        }
        val data = callback?.invoke() ?: NBTTagCompound()
        this.currentDirectory.addFile(File(fileName, data))
        return true
    }

    fun deleteFile(fileName: String): Boolean{
        val f = this.currentDirectory.files.stream().filter { it.name == fileName }.findFirst()
        if(f.isPresent){
            val file = f.get()
            if(file is Folder){
                val fit = file.files.iterator()
                for(ff in fit){
                    file.files.remove(ff)
                }
            }
            this.currentDirectory - file
            return true
        }
        return false
    }

    fun makeFile(fileName: String) = this.makeFile(fileName, null)

    fun writeToFile(fileName: String, callback: (fileData: NBTTagCompound) -> Unit): Boolean{
        if(currentDirectory.files.stream().anyMatch { it.name == fileName }){
            val file = currentDirectory.files.stream().filter { it.name == fileName }.findFirst().get()
            callback(file.data)
            return true
        }
        this.os.shell.printStringServer("Could not find file with name '$fileName; data was not written.", this.system.desktop.pos, this.system.desktop.player as EntityPlayerMP)
        return false
    }

    fun writeToFileWithPath(path: String, callback: (fileData: NBTTagCompound) -> Unit){
        val filePathPredicate: (file: File) -> Boolean = {file ->
            val fname = file.name
            val fpath = file.path
            val absPath = "$fpath/$fname"
            path == absPath
        }
        if(currentDirectory.files.stream().anyMatch(filePathPredicate)){
            val file = currentDirectory.files.stream().filter(filePathPredicate).findFirst().get()
            callback(file.data)
            return
        }
        this.os.shell.printStringServer("Could not find file with path '$path; data was not written.", this.system.desktop.pos, this.system.desktop.player as EntityPlayerMP)
    }

    fun serialize(): NBTTagCompound = root.data

    fun deserialize(nbt: NBTTagCompound){
        root = Folder("root", nbt)
    }
}

open class File constructor(val name: String, var data: NBTTagCompound){
    var parent: Folder? = null
    val path: String = "${parent?.path ?: ""}/$name"
}

open class Folder constructor(name: String, data: NBTTagCompound) : File(name, data){
    val files: ArrayList<File> = arrayListOf()

    init{
        deserialize(data)
    }

    fun addFile(file: File){
        files += file
        file.parent = this
        serializeAddedFile(file)
    }

    private fun serializeAddedFile(file: File){
        val data = file.data
        val type = if(file is Folder) "folder" else "file"
        val name = file.name
        val tag = NBTTagCompound()
        tag.setString("name", name)
        tag.setString("type", type)
        val parentTag = NBTTagCompound()
        parentTag.setString("name", this.parent?.name ?: "")
        parentTag.setTag("parentData", this.parent?.data ?: NBTTagCompound())
        tag.setTag("parent", parentTag)
        tag.setTag("data", data)
        this.data.setTag(name, tag)
    }

    fun deserialize(nbt: NBTTagCompound){
        for(k in nbt.keySet){
            val n = nbt.getCompoundTag(k)
            if(n.hasKey("name") && n.hasKey("type") && n.hasKey("data") && n.hasKey("parent")){
                val name = n.getString("name")
                val type = n.getString("type")
                val data = n.getCompoundTag("data")
                val parent = n.getCompoundTag("parent")
                if(type == "folder"){
                    val folder = Folder(name, data)
                    this.files += folder
                }else{
                    val file = File(name, data)
                    this.files += file
                }
                if(parent.hasKey("name") && parent.hasKey("parentData")){
                    val pname = parent.getString("name")
                    val pdata = parent.getCompoundTag("parentData")
                    val pfile = Folder(pname, pdata)
                    this.parent = pfile
                }
            }
        }
        this.data = nbt
    }

    operator fun plus(file: File){
        this.addFile(file)
    }

    fun removeFile(name: String){
        val file = this.files.stream().filter { it.name == name }.findFirst().get()
        this.removeFile(file)
    }

    fun removeFile(file: File){
        this.files -= file
        this.data.removeTag(file.name)
    }

    operator fun minus(file: File){
        this.removeFile(file)
    }

    operator fun minus(fileName: String){
        this.removeFile(fileName)
    }
}