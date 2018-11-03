package terminal

import blocks.TileEntityDesktopComputer
import modid
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.ResourceLocation
import utils.printstr
import messages.*
import net.minecraftforge.fml.relauncher.Side

interface TerminalCommand{
    val name: ResourceLocation
    val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit
}

object PackageManagerCommand : TerminalCommand{
    override val name: ResourceLocation = ResourceLocation(modid, "pakker")
    override val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit = {
        _, terminal, args ->
        if(args.size == 2) {
            if (args[0] == "-i") {
                terminal.packageManager.installPackage(args[1])
            }else{
                printstr("That is not a valid flag: ${args[0]}")
            }
        }

    }

}

object EchoCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "echo")
    override val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit
        get() = {player, terminal, args ->
            val sb = StringBuilder()
            args.forEachIndexed { i, s ->
                sb.append("$s${if(i == args.size-1) "" else " "}")
            }
            terminal.printStringServer(sb.toString(), player)
        }
}

object ListFilesCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "lf")
    override val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit
        get() = { player, terminal, _ ->
            val os = terminal.os
            val fs = os.fileSystem
            val files = fs.currentDirectory.files
            terminal.printStringServer("Files in current directory:", player)
            files.forEach {
                terminal.printStringServer("\t${it.name}", player)
            }
        }

}

object RelocateCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "rel")
    override val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit
        get() = {player, terminal, args ->
            if(args.size == 1){
                val name = args[0]
                if(terminal.os.fileSystem.relocate(name)){
                    printstr("Relocated to $name.", terminal)
                }
            }
        }

}

object ClearCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "clear")
    override val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit
        get() = {_,terminal, _ ->
            if(terminal is CouchTerminal){
                terminal.os.screen?.clearScreen()
            }
        }
}

object MakeFileCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "mkf")
    override val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit
        get() = { player, terminal, args ->
            if(args.size == 1){
                val name = args[0]
                terminal.os.fileSystem.makeFile(name)
                terminal.printStringServer("File with name '$name' created!", player)
            }else{
                terminal.printStringServer("Incorrect amount of args; should only take name of file.", player)
            }
        }
}

object MakeDirCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "mkd")
    override val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit
        get() = { player, terminal, args ->
            if(args.size == 1){
                val name = args[0]
                terminal.os.fileSystem.makeDirectory(name)
                terminal.printStringServer("Directory with name '$name' created!", player)
            }else{
                terminal.printStringServer("Incorrect amount of args; should only take name of directory.", player)
            }
        }
}

object DeleteFileCommand : TerminalCommand{
    override val name: ResourceLocation = ResourceLocation(modid, "delf")
    override val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit
            get() = { player, terminal, args ->
                if(args.size == 1){
                    val name = args[0]
                    terminal.os.fileSystem.deleteFile(name)
                    terminal.printStringServer("File with name '$name' deleted!", player)
                }else{
                    terminal.printStringServer("Incorrect amount of args; should only take name of file.", player)
                }
            }

}

object DeleteDirectoryCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "deld")
    override val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit
        get() = { player, terminal, args ->
            if(args.size == 1){
                val name = args[0]
                if(terminal.os.fileSystem.deleteFile(name)){
                    terminal.printStringServer("Directory with name '$name' created!", player)
                }
            }else{
                terminal.printStringServer("Incorrect amount of args; should only take name of file.", player)
            }
        }

}