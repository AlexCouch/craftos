package messages

import io.netty.buffer.ByteBuf
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.nbt.NBTUtil
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage

class TerminalExecuteCommandMessage() : IMessage {
    lateinit var command: String
    var args = arrayOf<String>()
    var pos = BlockPos(0,0,0)

    constructor(command: String, args: Array<String>, pos: BlockPos) : this(){
        this.command = command
        this.args = args
        this.pos = pos
    }

    override fun fromBytes(buf: ByteBuf) {
        this.command = ByteBufUtils.readUTF8String(buf)
        val argsTag = ByteBufUtils.readTag(buf) ?: return
        val argsArr = arrayListOf<String>()
        if(argsTag.hasKey("args")){
            val list = argsTag.getTagList("args", Constants.NBT.TAG_COMPOUND)
            list.forEachIndexed { i, n ->
                val tag = n as NBTTagCompound
                val arg = tag.getString("arg_$i")
                argsArr += arg
            }
        }
        this.args = argsArr.toTypedArray()
        this.pos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: return)
    }

    override fun toBytes(buf: ByteBuf) {
        ByteBufUtils.writeUTF8String(buf, command)
        val argsTag = NBTTagList()
        args.forEachIndexed{ index, str ->
            val nbt = NBTTagCompound()
            nbt.setString("arg_$index", str)
            argsTag.appendTag(nbt)
        }
        val nbt = NBTTagCompound()
        nbt.setTag("args", argsTag)
        ByteBufUtils.writeTag(buf, nbt)
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(this.pos))
    }
}

class SaveTermHistoryInMemory() : IMessage {
    var data = NBTTagCompound()
    var pos = BlockPos(0,0,0)

    constructor(lines: ArrayList<String>, desktopPos: BlockPos) : this(){
        val list = NBTTagList()
        for(l in lines){
            val tag = NBTTagString(l)
            list.appendTag(tag)
        }
        this.data.setTag("data", list)
        this.pos = desktopPos
    }
    override fun fromBytes(buf: ByteBuf) {
        this.data = ByteBufUtils.readTag(buf) ?: return
        this.pos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: return)
    }

    override fun toBytes(buf: ByteBuf) {
        ByteBufUtils.writeTag(buf, this.data)
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(pos))
    }
}

class LoadTermHistoryInStorageMessage() : IMessage {
    lateinit var nbt: NBTTagCompound
    var pos = BlockPos(0,0,0)

    constructor(nbt: NBTTagCompound, pos: BlockPos) : this(){
        this.nbt = nbt
        this.pos = pos
    }

    override fun fromBytes(buf: ByteBuf) {
        this.nbt = ByteBufUtils.readTag(buf) ?: NBTTagCompound()
        this.pos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: return)
    }

    override fun toBytes(buf: ByteBuf) {
        ByteBufUtils.writeTag(buf, this.nbt)
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(this.pos))
    }

}

class DisplayStringOnTerminal() : IMessage {
    var message: String = ""
    var pos = BlockPos(0,0,0)

    constructor(message: String, pos: BlockPos) : this(){
        this.message = message
        this.pos = pos
    }

    override fun fromBytes(buf: ByteBuf) {
        this.message = ByteBufUtils.readUTF8String(buf)
        this.pos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: return)
    }

    override fun toBytes(buf: ByteBuf) {
        ByteBufUtils.writeUTF8String(buf, this.message)
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(this.pos))
    }

}

class OpenTerminalGuiMessage(): IMessage {

    lateinit var pos: BlockPos

    constructor(pos: BlockPos) : this(){
        this.pos = pos
    }

    override fun fromBytes(buf: ByteBuf) {
        this.pos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: NBTTagCompound())
    }

    override fun toBytes(buf: ByteBuf) {
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(this.pos))
    }
}

class StartTerminalMessage(): IMessage {
    lateinit var blockpos: BlockPos

    constructor(blockpos: BlockPos): this(){
        this.blockpos = blockpos
    }

    override fun fromBytes(buf: ByteBuf){
        this.blockpos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: NBTTagCompound())
    }

    override fun toBytes(buf: ByteBuf){
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(this.blockpos))
    }
}

class UnlockBootScreenInputMessage(): IMessage {
    lateinit var blockpos: BlockPos

    constructor(blockpos: BlockPos): this(){
        this.blockpos = blockpos
    }

    override fun fromBytes(buf: ByteBuf){
        this.blockpos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: NBTTagCompound())
    }

    override fun toBytes(buf: ByteBuf){
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(this.blockpos))
    }
}

class OpenGuiMessage(): IMessage {
    var guiId = -1

    constructor(guiId: Int) : this(){
        this.guiId = guiId
    }

    override fun fromBytes(buf: ByteBuf) {
        this.guiId = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(this.guiId)
    }

}

class StartOSBootMessage() : IMessage {
    var blockpos = BlockPos.ORIGIN

    constructor(blockpos: BlockPos) : this(){
        this.blockpos = blockpos
    }

    override fun fromBytes(buf: ByteBuf){
        this.blockpos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: NBTTagCompound())
    }

    override fun toBytes(buf: ByteBuf){
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(this.blockpos))
    }
}

class InitializeOSMessage() : IMessage{
    var blockpos: BlockPos = BlockPos.ORIGIN

    constructor(blockpos: BlockPos) : this(){
        this.blockpos = blockpos
    }

    override fun fromBytes(buf: ByteBuf){
        this.blockpos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: NBTTagCompound())
    }

    override fun toBytes(buf: ByteBuf){
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(this.blockpos))
    }
}

class ChangeScreenModeMessage() : IMessage{
    var mode: Int = -1
    var blockpos: BlockPos = BlockPos.ORIGIN

    constructor(mode: Int, blockpos: BlockPos) : this(){
        this.mode = mode
        this.blockpos = blockpos
    }

    override fun fromBytes(buf: ByteBuf) {
        this.mode = buf.readInt()
        this.blockpos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: NBTTagCompound())
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(this.mode)
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(this.blockpos))
    }

}

class SyncFileSystemClientMessage() : IMessage{
    var blockpos: BlockPos = BlockPos.ORIGIN
    var fsdata = NBTTagCompound()

    constructor(blockpos: BlockPos, fsdata: NBTTagCompound) : this(){
        this.blockpos = blockpos
        this.fsdata = fsdata
    }

    override fun fromBytes(buf: ByteBuf) {
        this.blockpos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: NBTTagCompound())
        this.fsdata = ByteBufUtils.readTag(buf) ?: NBTTagCompound()
    }

    override fun toBytes(buf: ByteBuf) {
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(this.blockpos))
        ByteBufUtils.writeTag(buf, this.fsdata)
    }

}

class PrintToBootScreenMessage() : IMessage {
    var message: String = ""
    var pos = BlockPos(0,0,0)

    constructor(message: String, pos: BlockPos) : this(){
        this.message = message
        this.pos = pos
    }

    override fun fromBytes(buf: ByteBuf) {
        this.message = ByteBufUtils.readUTF8String(buf)
        this.pos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: return)
    }

    override fun toBytes(buf: ByteBuf) {
        ByteBufUtils.writeUTF8String(buf, this.message)
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(this.pos))
    }

}