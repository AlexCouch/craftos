package terminal

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import system.DeviceSystem
import terminal.errors.nullCommand
import terminal.errors.throwBadResponse
import terminal.messages.TerminalMessage

interface TerminalCommand{
    val name: ResourceLocation
    val execution: (DeviceSystem) -> TerminalResponse
    fun serialize(): NBTTagCompound
    fun deserialize(nbt: NBTTagCompound)
}

class TerminalStream{
    private var message: TerminalMessage? = null
    private val streamNetwork = NetworkRegistry.INSTANCE.newSimpleChannel("terminal_stream")

    var response: Any? = null
        get() = field ?: throwBadResponse(message?.command)

    fun sendCommand(command: TerminalCommand){
        message = TerminalMessage(command)
        sendMessage()
    }

    internal fun setResponse(response: (TerminalCommand)->Any){
        this.response = response(message?.command ?: throw nullCommand())
    }

    /**
     * Nobody can access this, so that server doesn't crash due to null message :D
     */
    private fun sendMessage(){
        streamNetwork.sendToServer(message ?: return)
    }
}

data class TerminalResponse(val code: Int, val message: String, val ret: Any)

class BadResponseException(reason: String, command: TerminalCommand) :
        Exception("${command.name} had a bad response: $reason")