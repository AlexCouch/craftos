package system

import blocks.TileEntityDesktopComputer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import os.OperatingSystem
import DevicesPlus
import com.teamwizardry.librarianlib.features.base.block.tile.TileMod
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.nbt.NBTUtil
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.text.TextComponentString
import net.minecraftforge.fml.client.config.GuiUtils
import network.Port
import os.couch.CouchOS
import stream
import terminal.CouchTerminal
import terminal.messages.OpenTerminalGuiMessage

@Savable
class CouchDesktopSystem(val desktop: TileEntityDesktopComputer) : DeviceSystem<TileEntityDesktopComputer>{
    override val os: OperatingSystem? = desktop.os
    override val memory = Memory(10000, NBTTagCompound())
    override val storage: NBTTagCompound
    get(){
        val tag = NBTTagCompound()
        tag.setString("name", this.name)
        tag.setTag("os", os?.serializeOS() ?: NBTTagCompound())
        tag.setTag("pos", NBTUtil.createPosTag(desktop.pos)) //Might be useful later
        return tag
    }
    override val name = "couch_desktop"
    override val te: TileEntityDesktopComputer = desktop
    override val ports: List<Port<*>> = ArrayList()

    lateinit var player: EntityPlayerMP

    override fun shutdown() {
        //Shutdown applications, wait til all have shutdown, and clear memory
        val programNBT = NBTTagCompound()
        if(this.os?.apps != null){
            for(program in this.os.apps){
                programNBT.setTag("programs", program.serialize())
                program.shutdown()
            }
        }
        memory.clear()
        Minecraft.getMinecraft().displayGuiScreen(null)
    }

    override fun start() {
        //load up ROM, and check all hardware for faults
        this.player.sendStatusMessage(TextComponentString("System started..."), true)
    }
}

@Savable
class Memory(@Save val space: Long, @Save var storedMemory: NBTTagCompound){
    init{
        prepareMemory()
    }

    fun prepareMemory(){
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

    //Do not ever call this unless you know what you're doing!!!
    fun clear(){
        storedMemory = NBTTagCompound()
        prepareMemory()
    }
}

@Savable
interface DeviceSystem<T : TileMod>{
    @Save
    val name: String
    @Save
    val memory: Memory
    @Save
    val storage: NBTTagCompound
    /**
     * This is nullable because of kernels (which will be implemented later on)
     */
    @Save
    val os: OperatingSystem?
    @Save
    val te: T
    @Save
    val ports: List<Port<*>>

    fun start()
    //TODO: Create system phases
//    fun idle()
    fun shutdown()
}