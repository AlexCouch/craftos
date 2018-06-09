package terminal.messages

import blocks.DesktopComputerBlock
import io.netty.buffer.ByteBuf
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import system.DeviceSystem
import terminal.TerminalResponse
import terminal.TerminalStream
import terminal.commands

class TerminalExecuteCommandMessage() : IMessage{
    lateinit var command: String
    var args = arrayOf<String>()

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
    }
}

class TerminalCommandResponseToClient() : IMessage{
    var response: TerminalResponse? = null

    constructor(response: TerminalResponse) : this(){
        this.response = response
    }

    override fun fromBytes(buf: ByteBuf) {
        val nbt = ByteBufUtils.readTag(buf) ?: return
        if(nbt.hasKey("code") && nbt.hasKey("message")){
            val code = nbt.getInteger("code")
            val message = nbt.getString("message")
            this.response = TerminalResponse(code, message)
        }
        println(this.response)
    }

    override fun toBytes(buf: ByteBuf) {
        println(this.response)
        val nbt = NBTTagCompound()
        nbt.setInteger("code", response?.code ?: return)
        nbt.setString("message", response?.message ?: return)
        ByteBufUtils.writeTag(buf, nbt)
    }

}

class SaveTermHistoryInMemory() : IMessage{
    var lines = NBTTagCompound()

    constructor(lines: ArrayList<String>) : this(){
        val list = NBTTagList()
        for(l in lines){
            val tag = NBTTagString(l)
            list.appendTag(tag)
        }
        this.lines.setTag("lines", list)
    }
    override fun fromBytes(buf: ByteBuf) {
        this.lines = ByteBufUtils.readTag(buf) ?: return
        println(this.lines)
    }

    override fun toBytes(buf: ByteBuf) {
        println(this.lines)
        ByteBufUtils.writeTag(buf, this.lines)
    }
}

class LoadTermHistoryInStorageMessage() : IMessage{
    lateinit var nbt: NBTTagCompound

    constructor(nbt: NBTTagCompound) : this(){
        this.nbt = nbt
    }

    override fun fromBytes(buf: ByteBuf) {
        this.nbt = ByteBufUtils.readTag(buf) ?: NBTTagCompound()
        println(this.nbt)
    }

    override fun toBytes(buf: ByteBuf) {
        println(this.nbt)
        ByteBufUtils.writeTag(buf, this.nbt)
    }

}

val saveTermHistoryInStorageHandler = IMessageHandler<SaveTermHistoryInMemory, LoadTermHistoryInStorageMessage>{ msg, ctx ->
    val termHistory = NBTTagCompound()
    termHistory.setTag("terminal_history", msg.lines)
    termHistory.setString("name", "terminal_history")
    DeviceSystem.memory.allocate(termHistory)
    LoadTermHistoryInStorageMessage(DeviceSystem.memory.referenceTo("terminal_history"))
}

val loadTermHistoryInStorageHandler = IMessageHandler<LoadTermHistoryInStorageMessage, IMessage>{ msg, ctx ->
    val list = msg.nbt.getTagList("lines", Constants.NBT.TAG_STRING)
    val history = arrayListOf<String>()
    list.forEach {
        history.add((it as NBTTagString).string)
    }
    TerminalStream.terminal.loadTerminalHistory(history)
    null
}

val terminalExecuteCommandMessage = IMessageHandler <TerminalExecuteCommandMessage, TerminalCommandResponseToClient>{ msg, ctx ->
    val command = commands[msg.command] ?: return@IMessageHandler null
    TerminalStream.executeCommand(ctx.serverHandler.player, command, DeviceSystem.os, msg.args)
    TerminalCommandResponseToClient(TerminalStream.response)
}

val terminalCommandResponseToClient = IMessageHandler<TerminalCommandResponseToClient, IMessage>{msg, _ ->
    DesktopComputerBlock.te?.postCommandResponse(msg.response ?: return@IMessageHandler null)
    null
}