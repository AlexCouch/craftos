package shell

import client.PrintableScreen
import messages.MessageFactory
import messages.ProcessData
import modid
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import system.CouchDesktopSystem
import utils.getCurrentComputer

interface TerminalCommand{
    val name: ResourceLocation
    val execute: (EntityPlayerMP, Shell, Array<String>) -> Unit
}

object PackageManagerCommand : TerminalCommand{
    override val name: ResourceLocation = ResourceLocation(modid, "pakker")
    override val execute: (EntityPlayerMP, Shell, Array<String>) -> Unit = {
        player, terminal, args ->
        if(args.size == 2) {
            if (args[0] == "-i") {
                terminal.packageManager.installPackage(args[1])
            }else{
                terminal.printStringServer("That is not a valid flag: ${args[0]}", terminal.os.system.te.pos, player)
            }
        }else{
            terminal.printStringServer("Incorrect amount of arguments; only 2 is required.", terminal.os.system.te.pos, player)
        }

    }

}

object EchoCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "echo")
    override val execute: (EntityPlayerMP, Shell, Array<String>) -> Unit
        get() = {player, terminal, args ->
            val sb = StringBuilder()
            args.forEachIndexed { i, s ->
                sb.append("$s${if(i == args.size-1) "" else " "}")
            }
            terminal.printStringServer(sb.toString(), terminal.os.system.te.pos, player)
        }
}

object ListFilesCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "ls")
    override val execute: (EntityPlayerMP, Shell, Array<String>) -> Unit
        get() = { player, terminal, _ ->
            val os = terminal.os
            val fs = os.fileSystem
            val files = fs.currentDirectory.files
            terminal.printStringServer("Files in current directory:", terminal.os.system.te.pos, player)
            files.forEach {
                terminal.printStringServer("\t${it.name}", terminal.os.system.te.pos, player)
            }
        }

}

object RelocateCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "cd")
    override val execute: (EntityPlayerMP, Shell, Array<String>) -> Unit
        get() = { player, terminal, args ->
            if(args.size == 1){
                val name = args[0]
                if(terminal.os.fileSystem.changeDirectory(name)){
                    terminal.printStringServer("Relocated to ${terminal.os.fileSystem.currentDirectory.path}.", terminal.os.system.te.pos, player)
                }
            }else{
                terminal.printStringServer("This command requires only one argument, you have ${args.size}.", terminal.os.system.te.pos, player)
            }
        }

}

object ClearCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "clear")
    override val execute: (EntityPlayerMP, Shell, Array<String>) -> Unit
        get() = {player ,terminal, _ ->
            if(terminal is CouchShell){
                val prepareData = { NBTTagCompound() }
                val processData: ProcessData = {_, world, pos, p ->
                    val te = getCurrentComputer(world, pos, p)!!
                    val screen = te.system.os?.screenAbstract!!
                    if(screen is PrintableScreen){
                        screen.clearScreen()
                    }
                }
                MessageFactory.sendDataToClient("clearCommand", player, terminal.os.system.te.pos, prepareData, processData)
            }
        }
}

object MakeFileCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "mkf")
    override val execute: (EntityPlayerMP, Shell, Array<String>) -> Unit
        get() = { player, terminal, args ->
            when {
                args.size == 1 -> {
                    val name = args[0]
                    terminal.os.fileSystem.makeFile(name, "text", true, null)
                    terminal.printStringServer("File with name '$name' created!", terminal.os.system.te.pos, player)
                }
                args.size == 2 -> {
                    val name: String =if(args[0].startsWith("-n=")) {
                        args[0].substring(0.."-n=".length)
                    }else{
                        terminal.printStringServer("Fist argument must be a file name; please use '-n=file_name'.", terminal.os.system.te.pos, player)
                        throw RuntimeException("First argument was not a file name.")
                    }
                    val type: String = if(args[1].startsWith("-t=")){
                        args[1].substring(0.."-t=".length)
                    }else{
                        terminal.printStringServer("Second argument must be a file type name; please use '-t=type_name'.", terminal.os.system.te.pos, player)
                        throw RuntimeException("Second argument was not a file type name.")
                    }
                    terminal.os.fileSystem.makeFile(name, type, true, null)
                }
                else -> terminal.printStringServer("Incorrect amount of args; should only take name of file.", terminal.os.system.te.pos, player)
            }
        }
}

object MakeDirCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "mkdir")
    override val execute: (EntityPlayerMP, Shell, Array<String>) -> Unit
        get() = { player, terminal, args ->
            if(args.size == 1){
                val name = args[0]
                terminal.os.fileSystem.makeDirectory(name, null)
                terminal.printStringServer("Directory with name '$name' created!", terminal.os.system.te.pos, player)
            }else{
                terminal.printStringServer("Incorrect amount of args; should only take name of directory.", terminal.os.system.te.pos, player)
            }
        }
}

object DeleteFileCommand : TerminalCommand{
    override val name: ResourceLocation = ResourceLocation(modid, "rm")
    override val execute: (EntityPlayerMP, Shell, Array<String>) -> Unit
            get() = { player, terminal, args ->
                if(args.size == 1){
                    val name = args[0]
                    terminal.os.fileSystem.deleteFile(name)
                    terminal.printStringServer("File with name '$name' deleted!", terminal.os.system.te.pos, player)
                }else{
                    terminal.printStringServer("Incorrect amount of args; should only take name of file.", terminal.os.system.te.pos, player)
                }
            }

}

object DeleteDirectoryCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "rmd")
    override val execute: (EntityPlayerMP, Shell, Array<String>) -> Unit
        get() = { player, terminal, args ->
            if(args.size == 1){
                val name = args[0]
                if(terminal.os.fileSystem.deleteFile(name)){
                    terminal.printStringServer("Directory with name '$name' created!", terminal.os.system.te.pos, player)
                }
            }else{
                terminal.printStringServer("Incorrect amount of args; should only take name of file.", terminal.os.system.te.pos, player)
            }
        }
}