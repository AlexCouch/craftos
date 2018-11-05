package system

import blocks.TileEntityDesktopComputer
import messages.MessageFactory
import messages.ProcessData
import net.minecraft.nbt.NBTTagCompound
import os.OperatingSystem
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.text.TextComponentString
import network.Port
import os.couch.*
import DevicesPlus
import net.minecraft.entity.player.EntityPlayerMP
import utils.getCurrentComputer

class CouchDesktopSystem(val desktop: TileEntityDesktopComputer) : DeviceSystem<TileEntityDesktopComputer>{
    override var os: OperatingSystem? = CouchOS(this)
    override var memory = Memory(10000, NBTTagCompound())
    override var storage: NBTTagCompound = NBTTagCompound()
    override var name = "couch_desktop"
    override var te: TileEntityDesktopComputer = desktop
    override var ports: List<Port<*>> = ArrayList()

    val player by lazy { desktop.player!! }

    override fun shutdown() {
        //Shutdown applications, wait til all have shutdown, and clear memory
        memory.clear()
    }

    override fun start() {
        //load up ROM, and check all hardware for faults
        this.player.sendStatusMessage(TextComponentString("System started..."), true)
        startOS()
    }

    private fun startOS(){
        val prepareMessageData = { NBTTagCompound() }
        val processMessageData: ProcessData = { _, world, pos, player ->
            player.openGui(DevicesPlus, 1, world, pos.x, pos.y, pos.z)
        }
        val prepareResponseData = { NBTTagCompound() }
        val processResponseData: ProcessData = { _, world, pos, player ->
            val comp = getCurrentComputer(world, pos, player)!!
            comp.system.os?.start()
        }
        MessageFactory.sendDataToClientWithResponse(
                this.desktop.pos,
                this.player as EntityPlayerMP,
                prepareMessageData,
                processMessageData,
                prepareResponseData,
                processResponseData
        )
    }

    override fun serialize(): NBTTagCompound {
        val tag = NBTTagCompound()
        tag.setString("name", this.name)
        tag.setTag("os", os?.serializeOS() ?: NBTTagCompound())
        tag.setTag("memory", this.memory.serialize())
        return tag
    }

    override fun deserialize(nbt: NBTTagCompound) {
        if(
                nbt.hasKey("name") &&
                nbt.hasKey("memory") &&
                nbt.hasKey("os")
        ){
            this.name = nbt.getString("name")
            val memorytag = nbt.getTag("memory") as NBTTagCompound
            if(memorytag.hasKey("space") && memorytag.hasKey("storedMemory")){
                val space = memorytag.getLong("space")
                val storedMemory = memorytag.getTag("storedMemory") as NBTTagCompound
                val memory = Memory(space, storedMemory)
                this.memory = memory
            }
            val osnbt = nbt.getCompoundTag("os")
            this.os?.deserializeOS(osnbt)
        }
    }
}

class Memory(val space: Long, var storedMemory: NBTTagCompound){
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

    fun serialize(): NBTTagCompound{
        val nbt = NBTTagCompound()
        nbt.setLong("space", this.space)
        nbt.setTag("memory", this.storedMemory)
        return nbt
    }
}

interface DeviceSystem<T : TileEntity>{
    var name: String
    var memory: Memory
    var storage: NBTTagCompound
    /**
     * This is nullable because of kernels (which will be implemented later on)
     */
    var os: OperatingSystem?
    var te: T
    var ports: List<Port<*>>

    fun start()
    //TODO: Create system phases
//    fun idle()
    fun shutdown()
    fun serialize(): NBTTagCompound
    fun deserialize(nbt: NBTTagCompound)
}