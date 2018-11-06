package utils

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.relauncher.Side
import terminal.Terminal
import java.io.OutputStream
import java.io.PrintStream

fun printstr(string: String, terminal: Terminal? = null){
    println(string)
    terminal?.os?.screen?.printToScreen(string)
}