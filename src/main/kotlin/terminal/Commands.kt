package terminal

import modid
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import os.OperatingSystem

val commands = HashMap<String, TerminalCommand>()

interface TerminalCommand{
    val name: ResourceLocation
    val execution: (EntityPlayerMP, OperatingSystem, Array<String>) -> TerminalResponse?
    fun serialize(): NBTTagCompound
    fun deserialize(nbt: NBTTagCompound)
}

object EchoCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "echo")
    override val execution: (EntityPlayerMP, OperatingSystem, Array<String>) -> TerminalResponse?
        get() = {_, _, args ->
            val sb = StringBuilder()
            args.forEach {
                sb.append("$it ")
            }
            TerminalResponse(0, sb.toString())
        }

    override fun serialize(): NBTTagCompound = NBTTagCompound()
    override fun deserialize(nbt: NBTTagCompound) {}
}

object ClearCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "clear")
    override val execution: (EntityPlayerMP, OperatingSystem, Array<String>) -> TerminalResponse?
        get() = {_,_, _ ->
            TerminalStream.terminal.modifyTerminalHistory {
                it.clear()
            }
            null
        }

    override fun serialize(): NBTTagCompound = NBTTagCompound()
    override fun deserialize(nbt: NBTTagCompound) {
    }

}