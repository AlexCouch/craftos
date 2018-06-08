package terminal

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.fml.common.network.NetworkRegistry
import os.OperatingSystem
import terminal.messages.TerminalCommandResponseToClient
import terminal.messages.TerminalExecuteCommandMessage

fun registerTerminalCommand(vararg _commands: TerminalCommand){
    for(command in _commands){
        commands[command.name.resourcePath] = command
    }
}

object TerminalStream{
    val streamNetwork = NetworkRegistry.INSTANCE.newSimpleChannel("terminal_stream")
    var objTransfer: Any? = null
    internal lateinit var response: TerminalResponse

    fun sendCommand(commandName: String, commandArgs: Array<String>){
        val message = TerminalExecuteCommandMessage()
        message.command = commandName
        message.args = commandArgs
        streamNetwork.sendToServer(message)
    }

    fun executeCommand(executor: EntityPlayerMP, command: TerminalCommand, os: OperatingSystem, args: Array<String>){
        println("Executing command: ${command.name}")
        response = command.execution(executor, os, args)
    }
}

data class TerminalResponse(val code: Int, val message: String)