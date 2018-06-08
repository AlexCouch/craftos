package terminal.errors

class BadResponseException(reason: String, message: String) :
        Exception("$reason: $message")

fun throwBadResponse(terminalCommand: String, code: Int){
    val message = when(code){
        0->{
            "$terminalCommand doesn't exist!"
        }
        1->{
            "$terminalCommand misused! See command manual for more!"
        }
        else -> {
            "$terminalCommand execution failed! See crash log for more info."
        }
    }
    throw BadResponseException("A terminal command returned with a bad response", message)
}

fun nullCommand(terminalCommand: String){
    throwBadResponse(terminalCommand, 0)
}