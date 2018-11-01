package terminal

import client.TerminalScreen
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import os.OperatingSystem
import terminal.messages.*
import utils.printlnstr

val terminalStream: SimpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("terminal_stream")

abstract class Terminal{
    abstract val commands: ArrayList<TerminalCommand>
    abstract val client: TerminalScreen?
    abstract fun registerCommand(command: TerminalCommand)
    abstract fun sendCommand(commandName: String, commandArgs: Array<String>)
    abstract fun getCommand(name: String): TerminalCommand
    abstract fun executeCommand(executor: EntityPlayerMP, command: TerminalCommand, args: Array<String>)
    abstract fun sendMessageToServer(message: IMessage)
    abstract fun sendMessageToClient(message: IMessage, player: EntityPlayerMP)
    abstract fun printStringServer(string: String, player: EntityPlayerMP)
    abstract fun printStringClient(string: String)
    abstract fun start(player: EntityPlayerMP)
}

class CouchTerminal : Terminal(){
    override val commands: ArrayList<TerminalCommand> = arrayListOf()

    override val client: TerminalScreen? by lazy {
        val screen = Minecraft.getMinecraft().currentScreen ?: return@lazy null
        if(screen is TerminalScreen){
            return@lazy screen as? TerminalScreen
        }
        return@lazy null
    }

    init{
        this.registerCommand(EchoCommand)
        this.registerCommand(ClearCommand)
    }

    override fun registerCommand(command: TerminalCommand) {
        this.commands += command
    }

    override fun getCommand(name: String): TerminalCommand = commands.stream().filter { it.name.resourcePath == name }.findFirst().get()

    override fun printStringServer(string: String, player: EntityPlayerMP) {
        this.sendMessageToClient(DisplayStringOnTerminal(string, client!!.te.pos), player)
    }

    override fun printStringClient(string: String) {
        this.client?.printToScreen(string)
    }

    override fun sendMessageToServer(message: IMessage){
        terminalStream.sendToServer(message)
    }

    override fun sendMessageToClient(message: IMessage, player: EntityPlayerMP){
        terminalStream.sendTo(message, player)
    }

    override fun sendCommand(commandName: String, commandArgs: Array<String>){
        terminalStream.sendToServer(TerminalExecuteCommandMessage(commandName, commandArgs, client!!.te.pos))
    }

    override fun executeCommand(executor: EntityPlayerMP, command: TerminalCommand, args: Array<String>){
        printlnstr("Executing command: ${command.name}")
        command.execute(executor, this, args)
    }

    override fun start(player: EntityPlayerMP) {
        terminalStream.registerMessage(terminalExecuteCommandMessage, TerminalExecuteCommandMessage::class.java, 0, Side.SERVER)
        terminalStream.registerMessage(saveTermHistoryInStorageHandler, SaveTermHistoryInMemory::class.java, 2, Side.SERVER)
        terminalStream.registerMessage(loadTermHistoryInStorageHandler, LoadTermHistoryInStorageMessage::class.java, 3, Side.CLIENT)
        terminalStream.registerMessage(displayStringOnTerminalHandler, DisplayStringOnTerminal::class.java, 4, Side.CLIENT)
    }
}