package utils

import terminal.Terminal
import java.io.OutputStream
import java.io.PrintStream

class TerminalPrintStream(var parentStream: PrintStream, val terminal: Terminal) : PrintStream(parentStream) {
    override fun print(s: String) {
        super.print(s)
        parentStream.print(s)
        terminal.printString(s, terminal.client.te.system!!.player)
    }

    override fun println(s: String) {
        super.print(s)
        parentStream.println(s)
        terminal.printString("$s\n", terminal.client.te.system!!.player)
    }
}