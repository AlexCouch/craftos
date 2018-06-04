package terminal.errors

import terminal.BadResponseException
import terminal.TerminalCommand

fun throwBadResponse(terminalCommand: TerminalCommand?){
    throw BadResponseException("A terminal command returned with a bad response", terminalCommand ?: throw nullCommand())
}

fun nullCommand(): Exception{
    return NullPointerException("Tried to use a command but was null!")
}