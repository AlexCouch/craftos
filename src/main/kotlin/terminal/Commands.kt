package terminal

import modid
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.ResourceLocation
import os.OperatingSystem

interface TerminalCommand{
    val name: ResourceLocation
    val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit
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

object ClearCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "clear")
    override val execute: (EntityPlayerMP, Terminal, Array<String>) -> Unit
        get() = {_,terminal, _ ->
            if(terminal is CouchTerminal){
                terminal.client?.modifyTerminalHistory {
                    it.clear()
                }
            }
        }
}