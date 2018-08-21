package client

import blocks.TileEntityDesktopComputer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Container
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.IGuiHandler
import org.lwjgl.input.Keyboard
import os.couch.CouchOS
import pkg.NetworkingPackage
import terminal.CouchTerminal
import terminal.messages.SaveTermHistoryInMemory
import utils.TerminalPrintStream
import java.awt.Color

class TerminalScreen(val te: TileEntityDesktopComputer) : GuiScreen(){
    private var w = 0.0
    private var h = 0.0
    private val x = 30.0
    private val y = 20.0
    private var cx = x + 10.0
    private var cy = y + 15

    private var lines = ArrayList<String>()

    lateinit var textField: GuiTextField
    val terminal = CouchTerminal(this)

    override fun initGui() {
        super.initGui()
        this.w = this.width - 35.0
        this.h = this.height - 35.0
        textField = GuiTextField(0, this.fontRenderer, this.cx.toInt(), this.cy.toInt(), this.w.toInt() - 25, this.h.toInt() - 25)
        textField.isFocused = true
        textField.enableBackgroundDrawing = false
        textField.text = "> "
        terminal.start()
        println(te.system)
        te.os = CouchOS(te.system!!, terminal)
        te.os?.registerPackage(NetworkingPackage())
        System.setOut(TerminalPrintStream(System.out, this.terminal))
        System.setErr(TerminalPrintStream(System.err, this.terminal))
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
        when(keyCode){
            Keyboard.KEY_RETURN -> {
                val text = textField.text
                lines.add(text)
                textField.text = "> "
                sendCommand(text)
                saveTermHistory()
                return
            }
        }
        this.textField.textboxKeyTyped(typedChar, keyCode)
    }

    fun sendCommand(commandStr: String){
        val commandSplit = commandStr.split(' ')
        val commandName = commandSplit[0]
        val argsArr = commandSplit.subList(1, commandSplit.size)
        terminal.sendCommand(commandName, argsArr.toTypedArray())
    }

    override fun doesGuiPauseGame(): Boolean = false

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)
        this.drawDefaultBackground()
        GlStateManager.color(0f, 0f, 0f)
        Gui.drawRect(this.x.toInt(), this.y.toInt(), this.w.toInt(), this.h.toInt(), Color.BLACK.rgb)
        println(this.textField)
        this.textField.drawTextBox()
        lines.forEachIndexed { index, s ->
            this.fontRenderer.drawString(s, cx.toInt(), cy.toInt() + (8 * index), Color.WHITE.rgb)
        }
    }

    fun saveTermHistory(){
        terminal.sendMessageToServer(SaveTermHistoryInMemory(lines, this.te.pos))
    }

    fun loadTerminalHistory(history: ArrayList<String>){
        this.lines = history
        resetCaretLocation()
    }

    fun modifyTerminalHistory(callback: (ArrayList<String>)->Unit){
        callback(this.lines)
        saveTermHistory()
    }

    fun resetCaretLocation(){
        textField.y = (y + 15 + (8 * this.lines.size)).toInt()
    }

    override fun updateScreen() {
        super.updateScreen()
        if(this::textField.isInitialized){
            this.textField.updateCursorCounter()
        }
    }
}

object GuiRegistry : IGuiHandler{
    override fun getClientGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any?{
        if(ID == 0){
            val te = Minecraft.getMinecraft().world.getTileEntity(BlockPos(x, y, z)) as TileEntityDesktopComputer
            return TerminalScreen(te)
        }
        return null
    }

    override fun getServerGuiElement(ID: Int, player: EntityPlayer?, world: World, x: Int, y: Int, z: Int): Any? = null

}