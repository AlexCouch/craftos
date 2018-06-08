package terminal

import blocks.DesktopComputerBlock
import modid
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import os.OperatingSystem
import system.DeviceSystem
import terminal.messages.LoadTermHistoryInStorageMessage
import terminal.messages.SaveTermHistoryInMemory

val commands = HashMap<String, TerminalCommand>()

interface TerminalCommand{
    val name: ResourceLocation
    val execution: (EntityPlayerMP, OperatingSystem, Array<String>) -> TerminalResponse
    fun serialize(): NBTTagCompound
    fun deserialize(nbt: NBTTagCompound)
}

object EchoCommand : TerminalCommand{
    override val name: ResourceLocation
        get() = ResourceLocation(modid, "echo")
    override val execution: (EntityPlayerMP, OperatingSystem, Array<String>) -> TerminalResponse
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
    override val execution: (EntityPlayerMP, OperatingSystem, Array<String>) -> TerminalResponse
        get() = {player,_, _ ->
            DeviceSystem.memory.deallocate("terminal_history")
            val nbt = NBTTagCompound()
            nbt.setString("name", "terminal_history")
            DeviceSystem.memory.allocate(nbt)
            TerminalStream.streamNetwork.sendTo(LoadTermHistoryInStorageMessage(DeviceSystem.memory.referenceTo("terminal_history")), player)
            TerminalResponse(0, "")
        }

    override fun serialize(): NBTTagCompound = NBTTagCompound()
    override fun deserialize(nbt: NBTTagCompound) {
    }

}