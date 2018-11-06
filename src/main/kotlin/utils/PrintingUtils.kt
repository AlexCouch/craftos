package utils

import shell.Shell

fun printstr(string: String, shell: Shell? = null){
    println(string)
    shell?.os?.screenAbstract?.printToScreen(string)
}