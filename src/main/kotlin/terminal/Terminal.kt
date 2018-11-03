package terminal

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side
import os.OperatingSystem
import os.couch.CouchOS
import pkg.*
import stream
import messages.*
import utils.printstr

val terminalStream: SimpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("terminal_stream")

abstract class Terminal(open val os: OperatingSystem){
    abstract val commands: ArrayList<TerminalCommand>
    abstract val packageManager: PackageManager

    abstract fun sendCommand(commandName: String, commandArgs: Array<String>)
    abstract fun sendMessageToServer(message: IMessage)
    abstract fun sendMessageToClient(message: IMessage, player: EntityPlayerMP)
    abstract fun printStringServer(string: String, player: EntityPlayerMP)
    abstract fun printStringClient(string: String)
    abstract fun start(player: EntityPlayerMP)

    open fun registerCommand(command: TerminalCommand){
        this.commands += command
    }

    open fun getCommand(name: String): TerminalCommand = commands.stream().filter { it.name.resourcePath == name }.findFirst().get()

    open fun getPackage(name: String): Package = packageManager.installedPackages.stream().filter { it.name == name }.findFirst().get()

    open fun verifyCommandOrPackage(commandName: String): Boolean = when {
        commands.stream().anyMatch { it.name.resourcePath == commandName } -> true
        packageManager.installedPackages.stream().anyMatch { it.name == commandName } -> false
        else -> {
            printstr("There is no command or package with that name: $commandName")
            false
        }
    }

    open fun openPackage(opener: EntityPlayerMP, pack: Package, args: Array<String>){
        pack.func(opener, this.os, arrayListOf(*args))
    }

    open fun executeCommand(executor: EntityPlayerMP, command: TerminalCommand, args: Array<String>){
        command.execute(executor, this, args)
    }
}

class CouchTerminal(override val os: CouchOS) : Terminal(os){
    override val commands: ArrayList<TerminalCommand> = arrayListOf()
    override val packageManager: PackageManager = PackageManager(this)

    init{
        this.registerCommand(EchoCommand)
        this.registerCommand(ClearCommand)
        this.registerCommand(PackageManagerCommand)
        this.registerCommand(RelocateCommand)
        this.registerCommand(MakeFileCommand)
        this.registerCommand(MakeDirCommand)
        this.registerCommand(DeleteFileCommand)
        this.registerCommand(DeleteDirectoryCommand)
        this.registerCommand(ListFilesCommand)
    }

    override fun printStringServer(string: String, player: EntityPlayerMP) {
        this.sendMessageToClient(DisplayStringOnTerminal(string, this.os.screen!!.te.pos), player)
    }

    override fun printStringClient(string: String) {
        this.os.screen!!.printToScreen(string)
    }

    override fun sendMessageToServer(message: IMessage){
        terminalStream.sendToServer(message)
    }

    override fun sendMessageToClient(message: IMessage, player: EntityPlayerMP){
        terminalStream.sendTo(message, player)
    }

    override fun sendCommand(commandName: String, commandArgs: Array<String>){
        terminalStream.sendToServer(TerminalExecuteCommandMessage(commandName, commandArgs, this.os.screen!!.te.pos))
    }

    override fun executeCommand(executor: EntityPlayerMP, command: TerminalCommand, args: Array<String>){
        printstr("Executing command: ${command.name}")
        command.execute(executor, this, args)
    }

    override fun start(player: EntityPlayerMP) {
        terminalStream.registerMessage(terminalExecuteCommandMessage, TerminalExecuteCommandMessage::class.java, 0, Side.SERVER)
        terminalStream.registerMessage(saveTermHistoryInStorageHandler, SaveTermHistoryInMemory::class.java, 1, Side.SERVER)
        terminalStream.registerMessage(loadTermHistoryInStorageHandler, LoadTermHistoryInStorageMessage::class.java, 2, Side.CLIENT)
        terminalStream.registerMessage(displayStringOnTerminalHandler, DisplayStringOnTerminal::class.java, 3, Side.CLIENT)
        terminalStream.registerMessage(syncFileSystemClientMessageHandler, SyncFileSystemClientMessage::class.java, 6, Side.CLIENT)
    }
}