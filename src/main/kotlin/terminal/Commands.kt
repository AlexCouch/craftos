package terminal

import modid
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.ResourceLocation
import os.OperatingSystem

interface TerminalCommand{
    val name: ResourceLocation
    val execute: (EntityPlayerMP, OperatingSystem, Array<String>) -> Unit
}

object EchoCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "echo")
    override val execute: (EntityPlayerMP, OperatingSystem, Array<String>) -> Unit
        get() = {_, _, args ->
            val sb = StringBuilder()
            args.forEachIndexed { i, s ->
                sb.append("$s${if(i == args.size-1) " " else ""}")
            }
        }
}

object ClearCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "clear")
    override val execute: (EntityPlayerMP, OperatingSystem, Array<String>) -> Unit
        get() = {_,os, _ ->
            if(os.terminal is CouchTerminal){
                val terminal = os.terminal as CouchTerminal
                terminal.client.modifyTerminalHistory {
                    it.clear()
                }
            }
        }
}