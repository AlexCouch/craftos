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

/**
 * This is an open generic class that takes a [String] name, [FileType] fileType, and [NBTTagCompound] data as constructor
 * parameters. This class is the backbone of all files and allows different kinds of [FileType] fileTypes to be used for
 * different purposes. This way you can make your own fileType that can be passed into this constructor.
 *
 * Generic Type T is the type of data that this file handles. This is then passed into the fileType object.
 * The [data] parameter holds the data that this file contains, which is used either externally for whatever purpose or
 * by the [fileType] object for reading and writing data.
 *
 * @see FileType
 */
open class File<T>(val name: String, val fileType: FileType<T>, var data: NBTTagCompound){
    var parent: Folder? = null
    val path: String = "${parent?.path ?: ""}/$name"

    init{
        data.setString("name", name)
        data.setString("path", path)
    }

    fun writeData(data: T){
        this.fileType.writeData(this, data)
    }

    fun readData(): T = this.fileType.readData(this)
}

/**
 * This is just a common file type wrapped as a simple class. Nothing too spicy going on here.
 */
class TextFile(name: String) : File<String>(name, FileTypeText, NBTTagCompound())

/**
 * This is an open class for creating folders. This takes a [String] [name] and [NBTTagCompound] [data] as constructor
 * parameters which is then passed into a File implementation that has a List<File<*>> type as T. The FileTypeFolder
 * serializes all the files in this class's [files] property into this class's [data].
 *
 * This class contains an [addFile] member function as well as a [removeFile] member function. These have their own
 * operator overloads: [plus] and [minus]. The [removeFile] function has two different implementations, one for taking a
 * file name and one for taking an actual file object.
 */
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