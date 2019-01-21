package os.filesystem

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants
import net.minecraftforge.common.util.Constants.NBT.TAG_STRING

abstract class FileType<T>{
    val data = NBTTagCompound()

    abstract val typeName: String
    abstract fun writeData(file: File<T>, data: T)
    abstract fun readData(file: File<T>): T
}

enum class FileTypes(val typeName: String, val type: FileType<*>){
    TEXT("text", FileTypeText),
    FOLDER("folder", FileTypeFolder);

}

fun getFileTypeByName(name: String): FileType<*> = FileTypes.values().toList().stream().filter { it.typeName == name }.findFirst().get().type

object FileTypeText : FileType<String>(){
    override val typeName: String = "text"

    override fun writeData(file: File<String>, data: String) {
        val nbt = NBTTagList()
        if(data.contains('\n')){
            val lines = data.split('\n')
            lines.forEach {
                nbt.appendTag(NBTTagString(it))
            }
        }
        file.data.setTag("text", nbt)
    }

    override fun readData(file: File<String>): String {
        val data = file.data
        if(data.hasKey("text")){
            val lines = data.getTagList("text", TAG_STRING)
            var string = ""
            lines.forEach {
                string += (it as NBTTagString).string
            }
            return string
        }
        return ""
    }

}

object FileTypeFolder : FileType<List<File<*>>>(){
    override val typeName: String
        get() = "folder"

    override fun writeData(file: File<List<File<*>>>, data: List<File<*>>) {
        val fdata = file.data
        data.forEach {
            if(!fdata.hasKey("files")){
                val list = NBTTagList()
                list.appendTag(serializeAddedFile(it))
                fdata.setTag("files", list)
            }else {
                val list = fdata.getTagList("files", Constants.NBT.TAG_COMPOUND)
                list.iterator().withIndex().forEach { (i, f) ->
                    if (it.name == (f as NBTTagCompound).getString("name")) {
                        list.removeTag(i)
                    }
                    list.appendTag(serializeAddedFile(it))
                }
            }
        }
    }

    private fun serializeAddedFile(file: File<*>): NBTTagCompound{
        val data = file.data
        val type = if(file is Folder) "folder" else (file as? File<*>)?.fileType?.typeName ?: "file"
        val name = file.name
        val tag = NBTTagCompound()
        tag.setString("name", name)
        tag.setString("type", type)
        val parentTag = NBTTagCompound()
        parentTag.setString("name", file.parent?.name ?: "")
        parentTag.setTag("parentData", file.parent?.data ?: NBTTagCompound())
        tag.setTag("parent", parentTag)
        tag.setTag("data", data)
        return tag
    }

    override fun readData(file: File<List<File<*>>>): List<File<*>>{
        val fdata = file.data
        var ret = emptyList<File<*>>()
        if(fdata.hasKey("files")){
            val flist = fdata.getTagList("files", Constants.NBT.TAG_COMPOUND)
            flist.forEach {
                val t = it as NBTTagCompound
                val fname = t.getString("name")
                val ftype = getFileTypeByName(t.getString("type"))
                val fd = t.getCompoundTag("data")
                val fparent = t.getCompoundTag("parent")
                val fpname = fparent.getString("name")
                val fpdata = fparent.getCompoundTag("data")
                val f = File(fname, ftype, fd)
                f.parent = Folder(fpname, fpdata)
                ret += f
            }
        }
        return ret
    }

}