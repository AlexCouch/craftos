package terminal.messages

import blocks.TileEntityDesktopComputer
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.nbt.NBTUtil
import net.minecraft.util.math.BlockPos
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
    }

    override fun toBytes(buf: ByteBuf) {
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
    }

    override fun toBytes(buf: ByteBuf) {
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

class StartTerminalMessage(): IMessage{
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

class PrintToLoadScreenMessage(): IMessage{
    var string = ""

    constructor(string: String) : this(){
        this.string = string
    }

    override fun fromBytes(buf: ByteBuf) {
        this.string = ByteBufUtils.readUTF8String(buf)
    }

    override fun toBytes(buf: ByteBuf?) {
        ByteBufUtils.writeUTF8String(buf, this.string)
    }
}

class OpenGuiMessage(): IMessage{
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

class StartOSBootMessage() : IMessage{
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

val startOSBootMessageHandler = IMessageHandler<StartOSBootMessage, InitializeOSMessage>{ msg, ctx ->
    val player = Minecraft.getMinecraft().player
    player.openGui(DevicesPlus, 0, player.world, msg.blockpos.x, msg.blockpos.y, msg.blockpos.z)
    InitializeOSMessage(msg.blockpos)
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

val initializeOSMessageHandler = IMessageHandler<InitializeOSMessage, IMessage>{ msg, ctx ->
    val comp = getCurrentComputer(ctx, msg.blockpos, ctx.side)
    comp.system.os?.start()
    null
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

class GetCurrentDirectoryFilesMessage() : IMessage{
    var blockpos: BlockPos = BlockPos.ORIGIN

    constructor(blockpos: BlockPos) : this(){
        this.blockpos = blockpos
    }

    override fun fromBytes(buf: ByteBuf) {
        this.blockpos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: NBTTagCompound())
    }

    override fun toBytes(buf: ByteBuf) {
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(this.blockpos))
    }

}

val getCurrentDirectoryFilesMessageHandler = IMessageHandler<GetCurrentDirectoryFilesMessage, PrintCurrentDirectoryFilesMessage>{ msg, ctx ->
    val comp = getCurrentComputer(ctx, msg.blockpos, ctx.side)
    val os = comp.system.os ?: return@IMessageHandler null
    val fs = os.fileSystem
    val files = fs.currentDirectory.files
    val filenames = arrayListOf<String>()
    files.forEach { filenames += it.name }
    PrintCurrentDirectoryFilesMessage(msg.blockpos, filenames)
}

class PrintCurrentDirectoryFilesMessage() : IMessage{
    var blockpos: BlockPos = BlockPos.ORIGIN
    val files = arrayListOf<String>()

    constructor(blockpos: BlockPos, files: ArrayList<String>) : this(){
        this.blockpos = blockpos
        this.files.addAll(files)
    }

    override fun fromBytes(buf: ByteBuf) {
        this.blockpos = NBTUtil.getPosFromTag(ByteBufUtils.readTag(buf) ?: NBTTagCompound())
        val filestag = ByteBufUtils.readTag(buf) ?: return
        filestag.keySet.forEach {
            val str = filestag.getString(it)
            this.files += str
        }
    }

    override fun toBytes(buf: ByteBuf) {
        ByteBufUtils.writeTag(buf, NBTUtil.createPosTag(this.blockpos))
        val filestag = NBTTagCompound()
        files.forEach {
            filestag.setTag(it, NBTTagString(it))
        }
        ByteBufUtils.writeTag(buf, filestag)
    }

}

val printCurrentDirectoryFilesMessageHandler = IMessageHandler<PrintCurrentDirectoryFilesMessage, IMessage>{msg, ctx ->
    val comp = getCurrentComputer(ctx, msg.blockpos, ctx.side)
    val os = comp.system.os ?: return@IMessageHandler null
    val fs = os.fileSystem
    val files = fs.currentDirectory.files
    os.terminal.printStringClient("Files in current directory:")
    files.forEach {
        os.terminal.printStringClient("\t${it.name}")
    }
    null
}

val changeScreenModeMessageHandler = IMessageHandler<ChangeScreenModeMessage, IMessage>{ msg, ctx ->
    val comp = getCurrentComputer(ctx, msg.blockpos, ctx.side)
//    comp.system.os?.screen?.mode = msg.mode
    null
}

fun getCurrentComputer(ctx: MessageContext, pos: BlockPos, side: Side): TileEntityDesktopComputer{
    val world = if(side == Side.SERVER) ctx.serverHandler.player.world else Minecraft.getMinecraft().world
    val te = world.getTileEntity(pos) as TileEntityDesktopComputer
    te.player = if(side == Side.SERVER) ctx.serverHandler.player else Minecraft.getMinecraft().player
    return te
}

val saveTermHistoryInStorageHandler = IMessageHandler<SaveTermHistoryInMemory, LoadTermHistoryInStorageMessage>{ msg, ctx ->
    val termHistory = NBTTagCompound()
    termHistory.setTag("terminal_history", msg.data)
    termHistory.setString("name", "terminal_history")
    val te = getCurrentComputer(ctx, msg.pos, Side.SERVER)
    LoadTermHistoryInStorageMessage(te.system.memory.pointerTo("terminal_history"), msg.pos)
}

val loadTermHistoryInStorageHandler = IMessageHandler<LoadTermHistoryInStorageMessage, IMessage>{ msg, ctx ->
    val list = msg.nbt.getTagList("data", Constants.NBT.TAG_STRING)
    val history = arrayListOf<String>()
    list.forEach {
        history.add((it as NBTTagString).string)
    }
    val te = getCurrentComputer(ctx, msg.pos, Side.CLIENT)
    val system = te.system
    system.os?.screen?.loadTerminalHistory(history)
    null
}

val terminalExecuteCommandMessage = IMessageHandler <TerminalExecuteCommandMessage, IMessage>{ msg, ctx ->
    val te = getCurrentComputer(ctx, msg.pos, ctx.side)
    val system = te.system
    val terminal = system.os?.terminal
    if(terminal?.verifyCommandOrPackage(msg.command) == true){
        val command = terminal.getCommand(msg.command)
        terminal.executeCommand(ctx.serverHandler.player, command, msg.args)
    }else{
        val pack = terminal?.getPackage(msg.command) ?: return@IMessageHandler null
        terminal.openPackage(ctx.serverHandler.player, pack, msg.args)
    }
    null
}

val displayStringOnTerminalHandler = IMessageHandler<DisplayStringOnTerminal, IMessage>{ msg, ctx ->
    val te = getCurrentComputer(ctx, msg.pos, Side.CLIENT)
    val system = te.system
    val os = system.os
    os?.screen?.printToScreen(msg.message)
    null
}

val openTerminalGuiMessageHandler = IMessageHandler<OpenTerminalGuiMessage, IMessage>{ msg, ctx ->
    val player = Minecraft.getMinecraft().player
    player.openGui(DevicesPlus, 0, Minecraft.getMinecraft().world, msg.pos.x, msg.pos.y, msg.pos.z)
    null
}

val startTerminalMessageHandler = IMessageHandler<StartTerminalMessage, OpenTerminalGuiMessage>{ msg, ctx ->
    val comp = getCurrentComputer(ctx, msg.blockpos, ctx.side)
    val system = comp.system
    val os = system.os ?: return@IMessageHandler null
    val terminal = os.terminal
    terminal.start(ctx.serverHandler.player)
    OpenTerminalGuiMessage(msg.blockpos)
}