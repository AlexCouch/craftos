package os.filesystem

import net.minecraft.nbt.NBTTagCompound

interface FileSystem {

    var currentDirectory: Folder

    /**
     * Changes the current directory
     * @param path - new reference directory
     */
    fun changeDirectory(path: String): Boolean
    /**
     * @param path - where to create the directory(including with the name)
     * @param callback - directory data
     */
    fun makeDirectory(path: String, callback: (() -> NBTTagCompound)?): Boolean
    /**
     * @param path - the location of the directory(including with the name)
     */
    fun deleteDirectory(path: String): Boolean

    /**
     * Sync the file system with the client
     */
    fun syncWithClient()

    /**
     * @param path - where to create the file(including with the name)
     * @param here - is path relative to root or the current directory
     * @param fileType - the name of the file type for this file
     * @param callback - file data
     */
    fun makeFile(path: String, fileType: String, here: Boolean = false, callback: (() -> NBTTagCompound)? = null): Boolean
    /**
     * @param path - the location of the file(including with the name)
     * @param here - is the path relative to the root or the current directory
     */
    fun deleteFile(path: String, here: Boolean = false): Boolean
    /**
     * Returns the file at the location specified
     * @param path - the location of the file(including with the name)
     * @param here - is the path relative to the root or the current directory
     */
    fun getFile(path: String, here: Boolean = false): File<*>?

    /**
     * Writes the data to the file at the location specified
     * @param path - the location of the file(including with the name)
     * @param callback - the data of the file
     */
    fun writeToFileWithPath(path: String, callback: () -> NBTTagCompound): Boolean

    /**
     * Checks if the file exists at the specified location
     * @param path - location of the file(including with the name)
     * @param here - is the path relative to the root or the current directory
     */
    fun doesFileExist(path: String, here: Boolean = false): Boolean

    /**
     * Sync the client file system with the server file system and serialize
     */
    fun serialize(): NBTTagCompound

    /**
     * Deserialize the current file system on the server and sync with the client
     */
    fun deserialize(nbt: NBTTagCompound)
}

open class File<T>(val name: String, val fileType: FileType<T>, var data: NBTTagCompound){
    var parent: Folder? = null
    val path: String = "${parent?.path ?: ""}/$name"

    init{
        data.setString("name", name)
        data.setString("path", path)
    }
}

class TextFile(name: String) : File<String>(name, FileTypeText, NBTTagCompound()){
    fun writeString(data: String) = this.fileType.writeData(this, data)
}

open class Folder constructor(name: String, data: NBTTagCompound) : File<List<File<*>>>(name, FileTypeFolder, data){
    val files: ArrayList<File<*>> = arrayListOf()

    fun addFile(file: File<*>){
        files += file
        file.parent = this
    }

    operator fun plus(file: File<*>){
        this.addFile(file)
    }

    fun removeFile(name: String){
        val file = this.files.stream().filter { it.name == name }.findFirst().get()
        this.removeFile(file)
    }

    fun removeFile(file: File<*>){
        this.files -= file
        this.data.removeTag(file.name)
    }

    operator fun minus(file: File<*>){
        this.removeFile(file)
    }

    operator fun minus(fileName: String){
        this.removeFile(fileName)
    }
}