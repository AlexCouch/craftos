package terminal

import blocks.DesktopComputerBlock
import client.TerminalScreen
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side
import os.OperatingSystem
import os.couch.CouchOS
import programs.Program
import programs.ProgramFunction
import programs.ProgramRenderer
import terminal.messages.*

abstract class Terminal{
    abstract val commands: ArrayList<TerminalCommand>
    abstract val stream: SimpleNetworkWrapper
    abstract val client: TerminalScreen
    abstract fun registerCommand(command: TerminalCommand)
    abstract fun sendCommand(commandName: String, commandArgs: Array<String>)
    abstract fun getCommand(name: String): TerminalCommand
    abstract fun executeCommand(executor: EntityPlayerMP, command: TerminalCommand, os: OperatingSystem, args: Array<String>)
    abstract fun sendMessageToServer(message: IMessage)
    abstract fun sendMessageToClient(message: IMessage, player: EntityPlayerMP)
    abstract fun printString(string: String, player: EntityPlayerMP)
    abstract fun start()
}

class CouchTerminal(gui: TerminalScreen) : Terminal(){
    override val commands: ArrayList<TerminalCommand>
        get() = arrayListOf()

    override val stream: SimpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("terminal_stream")
    override val client: TerminalScreen = gui

    override fun registerCommand(command: TerminalCommand) {
        this.commands + command
    }

    override fun getCommand(name: String): TerminalCommand = commands.filter { it.name.resourcePath == name }[0]

    override fun printString(string: String, player: EntityPlayerMP) {
        this.sendMessageToClient(DisplayStringOnTerminal(string, client.te.pos), player)
    }

    override fun sendMessageToServer(message: IMessage){
        stream.sendToServer(message)
    }

    override fun sendMessageToClient(message: IMessage, player: EntityPlayerMP){
        stream.sendTo(message, player)
    }

    override fun sendCommand(commandName: String, commandArgs: Array<String>){
        stream.sendToServer(TerminalExecuteCommandMessage(commandName, commandArgs, client.te.pos))
    }

    override fun executeCommand(executor: EntityPlayerMP, command: TerminalCommand, os: OperatingSystem, args: Array<String>){
        println("Executing command: ${command.name}")
        command.execute(executor, os, args)
    }

    override fun start() {
        commands.addAll(arrayListOf(EchoCommand, ClearCommand))
        this.stream.registerMessage(terminalExecuteCommandMessage, TerminalExecuteCommandMessage::class.java, 0, Side.SERVER)
        this.stream.registerMessage(saveTermHistoryInStorageHandler, SaveTermHistoryInMemory::class.java, 2, Side.SERVER)
        this.stream.registerMessage(loadTermHistoryInStorageHandler, LoadTermHistoryInStorageMessage::class.java, 3, Side.CLIENT)
        this.stream.registerMessage(displayStringOnTerminalHandler, DisplayStringOnTerminal::class.java, 4, Side.CLIENT)
    }
}