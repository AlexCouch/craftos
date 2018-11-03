package terminal

import modid
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.ResourceLocation

interface TerminalCommand{
    val name: ResourceLocation
    val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit
}

object PackageManagerCommand : TerminalCommand{
    override val name: ResourceLocation = ResourceLocation(modid, "pakker")
    override val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit = {
        player, terminal, args ->
        if(args.size == 2) {
            if (args[0] == "-i") {
                terminal.packageManager.installPackage(args[1])
            }else{
                terminal.printStringServer("That is not a valid flag: ${args[0]}", player)
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
        get() = ResourceLocation(modid, "ls")
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
        get() = ResourceLocation(modid, "cd")
    override val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit
        get() = { player, terminal, args ->
            if(args.size == 1){
                val name = args[0]
                if(terminal.os.fileSystem.relocate(name)){
                    terminal.printStringServer("Relocated to ${terminal.os.fileSystem.currentDirectory.path}.", player)
                }
            }else{
                terminal.printStringServer("This command requires only one argument, you have ${args.size}.", player)
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
        get() = ResourceLocation(modid, "mkfile")
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
        get() = ResourceLocation(modid, "mkdir")
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
    override val name: ResourceLocation = ResourceLocation(modid, "rm")
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
        get() = ResourceLocation(modid, "rmd")
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