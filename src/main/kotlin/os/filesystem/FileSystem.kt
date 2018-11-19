package os.filesystem

import blocks.TileEntityDesktopComputer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import os.OperatingSystem
import messages.*
import system.CouchDesktopSystem
import utils.getCurrentComputer
import kotlin.streams.toList

class FileSystem(private val os: OperatingSystem){
    private var root = Folder("root", NBTTagCompound())
    var currentDirectory = root

    private val system = this.os.system as CouchDesktopSystem

    fun changeDirectory(path: String): Boolean{
        if(path == ".."){
            currentDirectory = currentDirectory.parent ?: return false
            syncWithClient()
            return true
        }else if(path == root.path) {
            this.currentDirectory = root
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
        return true
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
        MessageFactory.sendDataToClient("syncFSClient", player, this.os.system.te.pos, prepareData, processData)
    }

    fun makeDirectory(path: String, callback: (() -> NBTTagCompound)?): Boolean{
        if(this.doesFileExist(path, false)){
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
            this.currentDirectory.addFile(SimpleFile(pathsplit[pathsplit.size - 1], callback?.invoke() ?: NBTTagCompound()))
            this.changeDirectory(currDir.path)
            return true
        }
        return false
    }

    fun makeDirectory(dirName: String) = makeDirectory(dirName, null)

    fun makeFile(path: String, here: Boolean = false, callback: (() -> NBTTagCompound)?): Boolean{
        if(this.doesFileExist(path, here)){
            return false
        }
        if(here){
            val file = SimpleFile(path, callback?.invoke() ?: NBTTagCompound())
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
                this.currentDirectory.addFile(SimpleFile(pathsplit[pathsplit.size - 1], callback?.invoke() ?: NBTTagCompound()))
                this.currentDirectory = currDir
                return true
            }
        }
        return false
    }

    fun deleteFile(path: String, here: Boolean = false): Boolean{
        val file = this.getFile(path, here) ?: return false
        val parent = file.parent ?: return false
        parent - file
        return false
    }

    fun doesFileExist(path: String, here: Boolean) = this.checkAndGetFile(path, here).first

    private fun checkAndGetFile(path: String, here: Boolean = false): Pair<Boolean, SimpleFile?>{
        var exists = false
        var file: SimpleFile? = null
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

    fun getFile(path: String, here: Boolean = false): SimpleFile? = this.checkAndGetFile(path, here).second

    fun doesFileExistHere(name: String) = this.doesFileExist(name, true)

    fun makeFileHere(fileName: String) = this.makeFile(fileName, true, null)

    fun writeToFile(fileName: String, callback: () -> NBTTagCompound): Boolean = this.writeToFileWithPath("${this.currentDirectory.path}/$fileName", callback)

    fun writeToFileWithPath(path: String, callback: () -> NBTTagCompound): Boolean{
        val file = this.getFile(path, false) ?: return false
        file.data = callback()
        return true
    }

    fun serialize(): NBTTagCompound = root.data

    fun deserialize(nbt: NBTTagCompound){
        root = Folder("root", nbt)
    }
}

open class SimpleFile(val name: String, var data: NBTTagCompound){
    var parent: Folder? = null
    val path: String = "${parent?.path ?: ""}/$name"

    init{
        data.setString("name", name)
        data.setString("path", path)
    }
}

open class File<T>(name: String, val fileType: FileType<T>, data: NBTTagCompound): SimpleFile(name, data){
    fun writeData(data: T){
        fileType.writeData(this, data)
    }
    fun readData(): T = fileType.readData(this)
}

class TextFile(name: String) : File<String>(name, FileTypeText, NBTTagCompound())

open class Folder constructor(name: String, data: NBTTagCompound) : SimpleFile(name, data){
    val files: ArrayList<SimpleFile> = arrayListOf()

    init{
        deserialize(data)
    }

    fun addFile(file: SimpleFile){
        files += file
        file.parent = this
        serializeAddedFile(file)
    }

    private fun serializeAddedFile(file: SimpleFile){
        val data = file.data
        val type = if(file is Folder) "folder" else (file as? File<*>)?.fileType?.typeName ?: "file"
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
                    val file = File(name, FileTypes.valueOf(type).type, data)
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

    operator fun plus(file: SimpleFile){
        this.addFile(file)
    }

    fun removeFile(name: String){
        val file = this.files.stream().filter { it.name == name }.findFirst().get()
        this.removeFile(file)
    }

    fun removeFile(file: SimpleFile){
        this.files -= file
        this.data.removeTag(file.name)
    }

    operator fun minus(file: SimpleFile){
        this.removeFile(file)
    }

    operator fun minus(fileName: String){
        this.removeFile(fileName)
    }
}