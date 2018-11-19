package os.filesystem

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants.NBT.TAG_STRING

interface FileType<T>{
    val typeName: String
    fun writeData(file: File<T>, data: T)
    fun readData(file: File<T>): T
}

enum class FileTypes(val typeName: String, val type: FileTypeText){
    TEXT("text", FileTypeText)
}

object FileTypeRegistry {

    var maps = HashMap<FileType<*>, Package>()

}

object FileTypeText : FileType<String>{
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