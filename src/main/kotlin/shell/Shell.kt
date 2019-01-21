package shell

import client.TerminalScreen
import net.minecraft.entity.player.EntityPlayerMP
import os.OperatingSystem
import os.couch.CouchOS
import pkg.*
import messages.*
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.util.Constants
import pkg.texteditor.TextEditorPackage
import system.CouchDesktopSystem
import utils.getCurrentComputer
import java.util.*

/**
 * An abstract class containing the basis for a shell which takes an [OperatingSystem] object as constructor parameter.
 *
 * This class contains registered commands ([commands]), a [PackageManager], a [sendCommand], [printStringServer],
 * [printStringClient], [start], [registerCommand], [getCommand], [isCommand], [isPackage], [openPackage], and [executeCommand].
 */
abstract class Shell(val os: OperatingSystem){
    /**
     * The list of registered commands.
     */
    abstract val commands: ArrayList<TerminalCommand>
    /**
     * The package manager. Must set this to an implementation of [PackageManager].
     */
    abstract val packageManager: PackageManager
    protected val system = os.system as CouchDesktopSystem

    /**
     * This function sends a command given the [commandName] with the [commandArgs]. This must first
     * check if the command exists and then send a message to the server telling it to execute said command.
     */
    abstract fun sendCommand(commandName: String, commandArgs: Array<String>)

    /**
     * This is a server side function that sends a message to the client to print a string to the shell screen gui.
     */
    abstract fun printStringServer(string: String, pos: BlockPos, player: EntityPlayerMP)

    /**
     * This is a client side function that prints a string to the shell screen gui.
     */
    abstract fun printStringClient(string: String)

    /**
     * This is the function that initializes everything that you need for your shell to work, such as registering commands,
     * sending signals to and fro the server/client for startup processes.
     */
    abstract fun start(player: EntityPlayerMP)

    /**
     * This is a basic open function that simply registers a [TerminalCommand]. This can be overridden for whatever purpose.
     */
    open fun registerCommand(command: TerminalCommand){
        this.commands += command
    }

    /**
     * This retrieves the registered command given the [name].
     */
    open fun getCommand(name: String): TerminalCommand = commands.stream().filter { it.name.resourcePath == name }.findFirst().get()

    /**
     * This checks if the given name matches any of the registered commands' names.
     */
    fun isCommand(name: String) = this.commands.stream().anyMatch { it.name.resourcePath == name }

    /**
     * This checks if the given name matches any of the registered packages in the package manager.
     */
    fun isPackage(name: String) = this.packageManager.isPackageInstalled(name)

    /**
     * This tells the package manager to open a package given the [name].
     */
    fun openPackage(name: String, args: Array<String>){
        val pack = packageManager.getInstalledPackage(name) ?: return
        pack.init(args)
    }

    /**
     * This is an open function that simply executes the given terminal command. This is done server side.
     */
    open fun executeCommand(executor: EntityPlayerMP, command: TerminalCommand, args: Array<String>){
        command.execute(executor, this, args)
    }
}

class CouchShell(os: CouchOS) : Shell(os){
    override val commands: ArrayList<TerminalCommand> = arrayListOf()
    override val packageManager: PackageManager = PackageManager(this)

    override fun printStringServer(string: String, pos: BlockPos, player: EntityPlayerMP) {
        val random = Random()
        val prepareData = {
            val nbt = NBTTagCompound()
            nbt.setString("string", string)
            nbt
        }
        val processData: ProcessData = { data, world, bp, p ->
            val str = data.getString("string")
            val te = getCurrentComputer(world, bp, p)!!
            te.system.os?.shell?.printStringClient(str)
        }
        MessageFactory.sendDataToClient("printStringServer_${random.nextInt()}", player, pos, prepareData, processData)
    }

    override fun printStringClient(string: String) {
        val screen = this.os.screenAbstract!!
        if(screen is TerminalScreen){
            screen.printToScreen(string)
        }
    }

    override fun sendCommand(commandName: String, commandArgs: Array<String>){
        val prepareData: () -> NBTTagCompound = {
            val nbt = NBTTagCompound()
            nbt.setString("name", commandName)
            val argsList = NBTTagList()
            for(a in commandArgs){
                argsList.appendTag(NBTTagString(a))
            }
            nbt.setTag("args", argsList)
            nbt
        }
        val processData: ProcessData = { data, world, pos, player ->
            if(data.hasKey("name") && data.hasKey("args")){
                val name = data.getString("name")
                val te = getCurrentComputer(world, pos, player)!!
                val argsList = data.getTagList("args", Constants.NBT.TAG_STRING)
                val args = arrayListOf<String>()
                for(a in argsList){
                    val str = (a as NBTTagString).string
                    args += str
                }
                val shell = te.system.os?.shell!!
                if(shell.isCommand(name)) {
                    val command = shell.getCommand(name)
                    shell.executeCommand(player as EntityPlayerMP, command, args.toTypedArray())
                }else if(shell.isPackage(name)){
                    shell.openPackage(name, args.toTypedArray())
                }
            }
        }
        MessageFactory.sendDataToServer("sendCommand", this.system.desktop.pos, prepareData, processData)
    }

    override fun start(player: EntityPlayerMP) {
        this.registerCommand(EchoCommand)
        this.registerCommand(ClearCommand)
        this.registerCommand(PackageManagerCommand)
        this.registerCommand(RelocateCommand)
        this.registerCommand(MakeFileCommand)
        this.registerCommand(MakeDirCommand)
        this.registerCommand(DeleteFileCommand)
        this.registerCommand(DeleteDirectoryCommand)
        this.registerCommand(ListFilesCommand)

        this.packageManager.registerPackage(TextEditorPackage(this.system))
        this.packageManager.installPackage("mcte")
    }
}