package client

import blocks.TileEntityDesktopComputer
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
abstract class AbstractSystemScreen(val system: CouchDesktopSystem) : GuiScreen(){
    var w: Int = 0
    var h: Int = 0
    abstract val x: Int
    abstract val y: Int

    val os = system.os!!
    val shell = os.shell

    abstract fun onInit()
    abstract fun onDraw()
    abstract fun onKeyTyped(typedChar: Char, keyCode: Int)
    abstract fun onUpdate()

    override fun initGui() {
        super.initGui()
        this.onInit()
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
        this.onKeyTyped(typedChar, keyCode)
    }

    override fun doesGuiPauseGame(): Boolean = false

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)
        this.drawDefaultBackground()
        this.onDraw()
    }

    override fun updateScreen() {
        super.updateScreen()
        this.onUpdate()
    }
}

abstract class PrintableScreen(system: CouchDesktopSystem) : AbstractSystemScreen(system){
    protected var cx: Int = 0
        get() = x + 10
    protected var cy: Int = 0
        get() = y + 15

    protected var lines = ArrayList<String>()

    fun printToScreen(string: String){
        if(string.contains("\t")){
            val new = string.substring(string.indexOf("\t") + 1)
            val tabbed = "  $new"
            lines.add(tabbed)
        }else{
            lines.add(string)
        }
    }

    open fun clearScreen(){
        this.lines.clear()
    }

    override fun onDraw() {
        GlStateManager.color(0f, 0f, 0f)
        Gui.drawRect(this.x, this.y, this.w, this.h, Color.BLACK.rgb)
//        this.textField.drawTextBox()
        val itr = lines.iterator()
        itr.withIndex().forEach{ i ->
            val (index, s) = i
            this.fontRenderer.drawString(s, cx, cy + (8 * index), Color.WHITE.rgb)
        }
    }

}

class BootScreen(system: CouchDesktopSystem) : PrintableScreen(system){
    override val x: Int = 30
    override val y: Int = 20

    var allowInput = false

    override fun onInit() {
        this.w = this.width - 35
        this.h = this.height - 35
    }

    override fun onKeyTyped(typedChar: Char, keyCode: Int) {
        if(allowInput){
            if(keyCode == Keyboard.KEY_RETURN){
                startShell()
            }
        }
    }

    override fun onUpdate() {}

    private fun startShell(){
        val prepareMessageData = { NBTTagCompound() }
        val processMessageData: ProcessData = { _, world, pos, player ->
            val comp = getCurrentComputer(world, pos, player)!!
            val terminal = comp.system.os!!.shell
            terminal.start(player as EntityPlayerMP)
        }
        val prepareResponseData = { NBTTagCompound() }
        val processResponseData: ProcessData = { _, world, pos, player ->
            GuiRegistry.openGui("terminal_screen", player, world, pos)
        }
        MessageFactory.sendDataToServerWithResponse(
                this.system.desktop.pos,
                prepareMessageData,
                processMessageData,
                prepareResponseData,
                processResponseData
        )
    }
}

class TerminalScreen(system: CouchDesktopSystem) : PrintableScreen(system){
    override val x: Int = 30
    override val y: Int = 20

    val textField: GuiTextField by lazy {
        GuiTextField(
                0,
                this.fontRenderer,
                this.cx,
                this.cy,
                this.w - 25,
                25
        )
    }

    val currentDirectory
        get() = os.fileSystem.currentDirectory

    var preText = "${this.currentDirectory.path} >"
        set(pt){
            val rawText = textField.text
            val currentText = rawText.substring(rawText.indexOf('>') + 1)
            textField.text = "$pt$currentText"
            field = pt
        }

    private val commandHistory = arrayListOf<String>()

    fun sendCommand(commandStr: String){
        val commandSplit = commandStr.split(' ')
        val commandName = commandSplit[0]
        val argsArr = commandSplit.subList(1, commandSplit.size)
        shell.sendCommand(commandName, argsArr.toTypedArray())
    }

    override fun onInit() {
        this.w = this.width - 35
        this.h = this.height - 35
        this.textField.isFocused = true
        this.textField.enableBackgroundDrawing = false
        this.textField.text = preText
    }

    override fun onKeyTyped(typedChar: Char, keyCode: Int) {
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
        }
        this.textField.textboxKeyTyped(typedChar, keyCode)
    }

    override fun onDraw() {
        super.onDraw()
        this.textField.drawTextBox()
        this.resetCaretLocation()
    }

    override fun onUpdate() {
        this.preText = "${this.currentDirectory.path} >"
        this.textField.updateCursorCounter()
    }

    fun resetCaretLocation(){
        textField.y = cy + (8 * this.lines.size)
    }

}

object GuiRegistry : IGuiHandler{
    private val registeredGuis = hashMapOf<String, Pair<Int, AbstractSystemScreen>>()
    private var id: Int = 0

    fun registerGui(name: String, gui: AbstractSystemScreen){
        registeredGuis[name] = (++id to gui)
    }

    fun openGui(name: String, player: EntityPlayer, world: World, pos: BlockPos){
        if(registeredGuis.contains(name)){
            val key = registeredGuis[name] ?: return
            val id = key.first
            player.openGui(DevicesPlus, id, world, pos.x, pos.y, pos.z)
        }
    }

    override fun getClientGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any?{
        val valueStream = registeredGuis.values.stream().filter { it.first == ID }.findFirst()
        if(valueStream.isPresent){
            val value = valueStream.get()
            return value.second
        }
        return null
    }

    override fun getServerGuiElement(ID: Int, player: EntityPlayer?, world: World, x: Int, y: Int, z: Int): Any? = null

}