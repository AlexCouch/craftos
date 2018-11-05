package client

import blocks.TileEntityDesktopComputer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.IGuiHandler
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.input.Keyboard
import java.awt.Color
import messages.*
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import DevicesPlus
import system.CouchDesktopSystem
import utils.getCurrentComputer

@SideOnly(Side.CLIENT)
class SystemScreen(val te: TileEntityDesktopComputer) : GuiScreen(){
    private var w = 0.0
    private var h = 0.0
    private val x = 30.0
    private val y = 20.0
    private var cx = x + 10.0
    private var cy = y + 15

    private var lines = ArrayList<String>()
    private var commandHistory = ArrayList<String>()

    val textField: GuiTextField by lazy {
        GuiTextField(
                0,
                this.fontRenderer,
                this.cx.toInt(),
                this.cy.toInt(),
                this.w.toInt() - 25,
                this.h.toInt() - 25
        )
    }

    val system = te.system
    val os = system.os!!
    val terminal = os.terminal
    val currentDirectory
        get() = os.fileSystem.currentDirectory

    var preText = "${this.currentDirectory.path} >"
        set(pt){
            val rawText = textField.text
            val currentText = rawText.substring(rawText.indexOf('>') + 1)
            textField.text = "$pt$currentText"
            field = pt
        }

    override fun initGui() {
        super.initGui()
        this.w = this.width - 35.0
        this.h = this.height - 35.0
        textField.isFocused = true
        textField.enableBackgroundDrawing = false
        textField.text = preText
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
        when (keyCode) {
            Keyboard.KEY_RETURN -> {
                val rawtext = textField.text
                val text = rawtext.substring(rawtext.lastIndexOf(preText) + preText.length until rawtext.length)
                if(rawtext != preText) {
                    lines.add(rawtext)
                    commandHistory.add(text)
                    sendCommand(text)
                }else{
                    lines.add(text)
                }
                textField.text = preText
                return
            }
            Keyboard.KEY_BACK -> {
                val text = textField.text
                if (text == preText) return
            }
            Keyboard.KEY_ESCAPE -> {
                shutdown()
            }
        }
        this.textField.textboxKeyTyped(typedChar, keyCode)
    }

    private fun shutdown(){
        val prepareData = { NBTTagCompound() }
        val processData: ProcessData = { _, world, pos, player ->
            val comp = getCurrentComputer(world, pos, player)!!
            comp.started = false
        }
    }

    fun printToScreen(string: String){
        if(string.contains("\t")){
            val new = string.substring(string.indexOf("\t") + 1)
            val tabbed = "  $new"
            lines.add(tabbed)
        }else{
            lines.add(string)
        }
    }

    fun clearScreen(){
        this.commandHistory.clear()
        this.lines.clear()
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
        this.textField.drawTextBox()
        val itr = lines.iterator()
        itr.withIndex().forEach{ i ->
            val (index, s) = i
            this.fontRenderer.drawString(s, cx.toInt(), cy.toInt() + (8 * index), Color.WHITE.rgb)
        }
        this.resetCaretLocation()
    }

    fun resetCaretLocation(){
        textField.y = (y + 15 + (8 * this.lines.size)).toInt()
    }

    override fun updateScreen() {
        super.updateScreen()
        this.preText = "${this.currentDirectory.path} >"
        this.textField.updateCursorCounter()
    }
}

@SideOnly(Side.CLIENT)
class BootScreen(private val system: CouchDesktopSystem) : GuiScreen(){
    private val x = 30
    private val y = 20
    private val w by lazy{ this.width - 35 }
    private val h by lazy{ this.height - 35 }
    private val cx = x + 10
    private val cy = y + 15
    private val lines = arrayListOf<String>()

    var allowInput = false

    fun printToScreen(string: String){
        if(string.startsWith("\n")){
            string.replace("\n", "  ")
        }
        lines += string
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if(allowInput){
            if(keyCode == Keyboard.KEY_RETURN){
                startTerminal()
            }
        }
    }

    private fun startTerminal(){
        val prepareMessageData = { NBTTagCompound() }
        val processMessageData: ProcessData = { _, world, pos, player ->
            val comp = getCurrentComputer(world, pos, player)!!
            val terminal = comp.system.os!!.terminal
            terminal.start(player as EntityPlayerMP)
        }
        val prepareResponseData = { NBTTagCompound() }
        val processResponseData: ProcessData = { _, world, pos, player ->
            player.openGui(DevicesPlus, 0, world, pos.x, pos.y, pos.z)
        }
        MessageFactory.sendDataToServerWithResponse(
                this.system.desktop.pos,
                prepareMessageData,
                processMessageData,
                prepareResponseData,
                processResponseData
        )
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        GlStateManager.color(0f, 0f, 0f)
        Gui.drawRect(x, y, this.w, this.h, Color.BLACK.rgb)

        lines.iterator().withIndex().forEach {
            val (i, s) = it
            this.fontRenderer.drawString(s, cx, cy + (8 * i), Color.WHITE.rgb)
        }
    }
}

object GuiRegistry : IGuiHandler{
    override fun getClientGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any?{
        val te = getCurrentComputer(world, BlockPos(x, y, z), player) ?: return null
        if(ID == 0){
            return SystemScreen(te)
        }else if(ID == 1){
            return BootScreen(te.system)
        }
        return null
    }

    override fun getServerGuiElement(ID: Int, player: EntityPlayer?, world: World, x: Int, y: Int, z: Int): Any? = null

}