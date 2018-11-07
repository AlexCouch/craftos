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
import system.CouchDesktopSystem
import utils.getCurrentComputer

abstract class Shell(val os: OperatingSystem){
    abstract val commands: ArrayList<TerminalCommand>
    abstract val packageManager: PackageManager
    protected val system = os.system as CouchDesktopSystem

    abstract fun sendCommand(commandName: String, commandArgs: Array<String>)
    abstract fun printStringServer(string: String, pos: BlockPos, player: EntityPlayerMP)
    abstract fun printStringClient(string: String)
    abstract fun start(player: EntityPlayerMP)

    open fun registerCommand(command: TerminalCommand){
        this.commands += command
    }

    open fun getCommand(name: String): TerminalCommand = commands.stream().filter { it.name.resourcePath == name }.findFirst().get()

    fun isCommand(name: String) = this.commands.stream().anyMatch { it.name.resourcePath == name }
    fun isPackage(name: String) = this.packageManager.isPackageInstalled(name)

    fun openPackage(name: String){
        val pack = packageManager.getInstalledPackage(name) ?: return
        pack.init()
    }

    open fun executeCommand(executor: EntityPlayerMP, command: TerminalCommand, args: Array<String>){
        command.execute(executor, this, args)
    }
}

class CouchShell(os: CouchOS) : Shell(os){
    override val commands: ArrayList<TerminalCommand> = arrayListOf()
    override val packageManager: PackageManager = PackageManager(this)

    override fun printStringServer(string: String, pos: BlockPos, player: EntityPlayerMP) {
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
        MessageFactory.sendDataToClient(player, pos, prepareData, processData)
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
                    shell.openPackage(name)
                }
            }
        }
        MessageFactory.sendDataToServer(this.system.desktop.pos, prepareData, processData)
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
    }
}