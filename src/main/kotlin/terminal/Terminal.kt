package terminal

import net.minecraft.entity.player.EntityPlayerMP
import os.OperatingSystem
import os.couch.CouchOS
import pkg.*
import messages.*
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants
import utils.getCurrentComputer
import utils.printstr

abstract class Terminal(open val os: OperatingSystem){
    abstract val commands: ArrayList<TerminalCommand>
    abstract val packageManager: PackageManager

    abstract fun sendCommand(commandName: String, commandArgs: Array<String>)
    abstract fun printStringServer(string: String, pos: BlockPos, player: EntityPlayerMP)
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

    override fun printStringServer(string: String, pos: BlockPos, player: EntityPlayerMP) {
        val prepareData = {
            val nbt = NBTTagCompound()
            nbt.setString("string", string)
            nbt
        }
        val processData: ProcessData = { data, world, bp, p ->
            val str = data.getString("string")
            val te = getCurrentComputer(world, bp, p)!!
            val screen = te.system.os!!.screen!!
            screen.printToScreen(str)
        }
        MessageFactory.sendDataToClient(player, pos, prepareData, processData)
    }

    override fun printStringClient(string: String) {
        this.os.screen!!.printToScreen(string)
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
        val processData: (data: NBTTagCompound, world: World, pos: BlockPos, player: EntityPlayer) -> Unit = { data, world, pos, player ->
            if(data.hasKey("name") && data.hasKey("args")){
                val name = data.getString("name")
                val te = getCurrentComputer(world, pos, player)!!
                val argsList = data.getTagList("args", Constants.NBT.TAG_STRING)
                val args = arrayListOf<String>()
                for(a in argsList){
                    val str = (a as NBTTagString).string
                    args += str
                }
                val terminal = te.system.os?.terminal!!
                val command = terminal.getCommand(name)
                te.system.os?.terminal?.executeCommand(player as EntityPlayerMP, command, args.toTypedArray())
            }
        }
        MessageFactory.sendDataToServer(this.os.screen!!.te.pos, prepareData, processData)
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