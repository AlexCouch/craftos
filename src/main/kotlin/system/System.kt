package system

import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import os.OperatingSystem
import os.katt.KattOS

object DeviceSystem{
    var os: OperatingSystem = KattOS()
    var memory = Memory(10000, NBTTagCompound())
}

class Memory(private val space: Long, private val storedMemory: NBTTagCompound){
    init{
        storedMemory.setLong("maxmem", space)
        storedMemory.setLong("availmem", space)
    }

    /**
     * Checks if there is enough memory then decreases the memory size
     */
    private fun validateMemory(requestSize: Int){
        if(storedMemory.getLong("availmem") <= requestSize) throw IllegalStateException("Not enough memory in system!")
        storedMemory.removeTag("availmem")
        storedMemory.setLong("availmem", this.space - requestSize)
    }

    /**
     * Allocates memory (nbt) to the given object tag.
     * @param objTag a tag compound that contains a string tag (name) and whatever serialized object you want (nbt)
     */
    fun allocate(objTag: NBTTagCompound){
        validateMemory(objTag.size)
        val name = objTag.getString("name")
        val obj = objTag.getCompoundTag(name)
        storedMemory.setTag(name, obj)
    }

    /**
     * This is for non-empty tags in memory.
     * This will throw an exception if you reference an empty tag
     */
    fun referenceTo(name: String): NBTTagCompound{
        if(storedMemory.hasKey(name)){
            return storedMemory.getCompoundTag(name)
        }
        throw IllegalStateException("Referencing non-existent object in memory: $name")
    }

    /**
     * A pointer to an object in memory that may or may not be an empty tag, but will be allocated for you to fill.
     */
    fun pointerTo(name: String): NBTTagCompound {
        return if(storedMemory.hasKey(name)){
            storedMemory.getCompoundTag(name)
        }else{
            val tag = NBTTagCompound()
            tag.setString("name", name)
            allocate(tag)
            tag
        }
    }

    /**
     * Deallocates an object from memory, if it exists; otherwise, throws an illegal state exception
     */
    fun deallocate(name: String){
        if(storedMemory.hasKey(name)){
            storedMemory.removeTag(name)
            return
        }
        throw IllegalStateException("Trying to deallocate non-existent tag: $name")
    }
}