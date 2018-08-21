package terminal.messages

import blocks.TileEntityDesktopComputer
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.nbt.NBTUtil
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.relauncher.Side
import DevicesPlus

class TerminalExecuteCommandMessage() : IMessage{
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
        println(this.command)
        println(this.args.toString())
    }

    override fun toBytes(buf: ByteBuf) {
        println(this.command)
        println(this.args.toString())
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

class SaveTermHistoryInMemory() : IMessage{
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
        println(this.data)
        println(this.pos)
    }

    override fun toBytes(buf: ByteBuf) {
        println(this.data)
        println(this.pos)
        ByteBufUtils.writeTag(buf, this.data)
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(pos))
    }
}

class LoadTermHistoryInStorageMessage() : IMessage{
    lateinit var nbt: NBTTagCompound
    var pos = BlockPos(0,0,0)

    constructor(nbt: NBTTagCompound, pos: BlockPos) : this(){
        this.nbt = nbt
        this.pos = pos
    }

    override fun fromBytes(buf: ByteBuf) {
        this.nbt = ByteBufUtils.readTag(buf) ?: NBTTagCompound()
        this.pos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: return)
        println(this.nbt)
    }

    override fun toBytes(buf: ByteBuf) {
        println(this.nbt)
        ByteBufUtils.writeTag(buf, this.nbt)
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(this.pos))
    }

}

class DisplayStringOnTerminal() : IMessage{
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

class OpenTerminalGuiMessage(): IMessage{

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

fun getCurrentComputer(ctx: MessageContext, pos: BlockPos, side: Side): TileEntityDesktopComputer{
    val world = if(side == Side.SERVER) ctx.serverHandler.player.world else Minecraft.getMinecraft().world
    return world.getTileEntity(pos) as TileEntityDesktopComputer
}

val saveTermHistoryInStorageHandler = IMessageHandler<SaveTermHistoryInMemory, LoadTermHistoryInStorageMessage>{ msg, ctx ->
    val termHistory = NBTTagCompound()
    termHistory.setTag("terminal_history", msg.data)
    termHistory.setString("name", "terminal_history")
    val te = getCurrentComputer(ctx, msg.pos, Side.SERVER)
    LoadTermHistoryInStorageMessage(te.system!!.memory.referenceTo("terminal_history"), msg.pos)
}

val loadTermHistoryInStorageHandler = IMessageHandler<LoadTermHistoryInStorageMessage, IMessage>{ msg, ctx ->
    val list = msg.nbt.getTagList("data", Constants.NBT.TAG_STRING)
    val history = arrayListOf<String>()
    list.forEach {
        history.add((it as NBTTagString).string)
    }
    val te = getCurrentComputer(ctx, msg.pos, Side.CLIENT)
    val system = te.system!!
    system.os?.terminal?.client?.loadTerminalHistory(history)
    null
}

val terminalExecuteCommandMessage = IMessageHandler <TerminalExecuteCommandMessage, IMessage>{ msg, ctx ->
    val world = ctx.serverHandler.player.world
    val te = world.getTileEntity(msg.pos) as TileEntityDesktopComputer
    val system = te.system
    val terminal = system?.os?.terminal!!
    val command = terminal.getCommand(msg.command)
    terminal.executeCommand(ctx.serverHandler.player, command, system.os, msg.args)
    null
}

val displayStringOnTerminalHandler = IMessageHandler<DisplayStringOnTerminal, IMessage>{ msg, ctx ->
    val te = getCurrentComputer(ctx, msg.pos, Side.CLIENT)
    te.os?.terminal!!.client.modifyTerminalHistory { it.add(msg.message) }
    null
}

val openTerminalGuiMessageHandler = IMessageHandler<OpenTerminalGuiMessage, IMessage>{ msg, ctx ->
    val player = Minecraft.getMinecraft().player
    player.openGui(DevicesPlus, 0, Minecraft.getMinecraft().world, msg.pos.x, msg.pos.y, msg.pos.z)
    null
}