package terminal.messages

import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import terminal.TerminalCommand

class TerminalMessage() : IMessage{
    var command: TerminalCommand? = null
    constructor(command: TerminalCommand) : this(){
        this.command = command
    }

    override fun fromBytes(buf: ByteBuf?) {

    }

    override fun toBytes(buf: ByteBuf?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}