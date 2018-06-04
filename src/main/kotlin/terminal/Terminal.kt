package terminal

import com.google.common.reflect.TypeToken
import com.google.gson.*
import os.OperatingSystem
import system.DeviceSystem
import java.lang.reflect.Type

class Terminal(val os: OperatingSystem, val stream: TerminalStream){
    fun executeCommand(command: TerminalCommand){

        command.execution(DeviceSystem)
    }

    fun start(){
        os.commands.forEach {
            stream.sendCommand(it)
        }
    }
}