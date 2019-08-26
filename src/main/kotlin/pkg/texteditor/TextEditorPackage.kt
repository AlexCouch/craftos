package pkg.texteditor

import pkg.*
import client.AbstractSystemScreen
import messages.MessageFactory
import messages.ProcessData
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.Gui
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants.NBT.TAG_STRING
import org.lwjgl.input.Keyboard
import os.filesystem.FileTypes
import os.filesystem.TextFile
import system.CouchDesktopSystem
import java.awt.Color

class ScrollableTextField(
        val fontRenderer: FontRenderer,
        val guiTextEditor: GuiTextEditor
) : Gui(){
    private var x = 0
    private var y = 0
    private var w = 0
    private var h = 0

    private var cpos = 0
        set(np){
            when {
                np >= this.linesCap -> scroll++
                np <= 0 -> {field = 0; scroll--}
                np >= this.lines.size -> field = this.lines.size - 1
                else -> field = np
            }
        }

    private val cx
        get() = x + 15
    private val cy
        get() = y + 15
    private val textField: MouselessTextField
        by lazy {
            MouselessTextField(
                    0,
                    fontRenderer,
                    cx,
                    cy,
                    w - 5,
                    10
            )
        }

    private val scrollTop
        get() = scroll
    private val scrollBottom: Int
        get() = scrollTop + linesCap

    private var scroll = 0
        set(ns){
            if(ns > scroll){
                if(scrollBottom >= this.lines.size) return
            }else if(ns < scroll){
                if(scrollTop <= 0) return
            }
            field = ns
        }

    private var linesCap: Int = -1

    //Initialize new array list of a single blank/empty string
    val lines = arrayListOf("")
    var currentFileName: String = ""

    fun init(x: Int, y: Int, w: Int, h: Int){
        this.x = x
        this.y = y
        this.w = w
        this.h = h
        this.textField.isFocused = true
        this.textField.enableBackgroundDrawing = false
        this.textField.maxStringLength = this.w / this.fontRenderer.getCharWidth('a')
        this.linesCap = this.h / this.textField.height
    }

    fun keyTyped(typedChar: Char, keyCode: Int){
        when{
            keyCode == Keyboard.KEY_UP -> {
                moveLine()
            }
            keyCode == Keyboard.KEY_DOWN -> {
                moveLine(false)
            }
            keyCode == Keyboard.KEY_RETURN -> {
                createNewLineAndMove()
            }
            keyCode == Keyboard.KEY_BACK -> {
                when {
                    this.textField.text.isBlank() -> {
                        if(cpos > 0) {
                            this.textField.text = this.lines[cpos + scroll]
                            val next = this.lines[cpos + scroll - 1]
                            this.lines.removeAt(cpos + scroll)
                            moveLine()
                            this.textField.cursorPosition = next.length + 1
                        }
                    }
                    this.textField.cursorPosition == 0 -> {
                        val currLine = this.textField.text
                        val prevLine = this.lines[cpos + scroll - 1]
                        val merged = prevLine + currLine
                        if(merged.length > this.textField.maxStringLength){
                            val cutBefore = merged.substring(0, this.textField.maxStringLength)
                            val cutAfter = merged.substring(this.textField.maxStringLength)
                            cpos--
                            this.lines[cpos + scroll] = ""
                            this.textField.text = cutBefore
                            this.lines[cpos+scroll+1] = cutAfter
                            this.textField.cursorPosition = prevLine.length
                        }else{
                            this.lines.removeAt(cpos + scroll - 1)
                            this.textField.text = merged
                            cpos--
                            this.textField.cursorPosition = prevLine.length
                        }
                    }
                    else -> this.textField.textboxKeyTyped(typedChar, keyCode)
                }
            }
            keyCode == Keyboard.KEY_S && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) -> {
                saveFile()
            }
            keyCode == Keyboard.KEY_RETURN && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) -> {
                this.guiTextEditor.textEditor.exit()
            }
            else -> {
                if(this.textField.cursorPosition == this.textField.maxStringLength){
                    createNewLineAndMove()
                }
                this.textField.textboxKeyTyped(typedChar, keyCode)
            }
        }
    }

    private fun saveFile(){
        val data = StringBuilder()
        this.lines.forEach {
            data.append(it)
            data.append("\n")
        }
        this.guiTextEditor.saveFile(data.toString())
    }

    private fun createNewLineAndMove(){
        if(this.textField.y <= (this.h - this.textField.height)) {
            if (cpos + scroll < this.lines.size)
                this.lines.add(cpos + scroll, this.textField.text)
            else
                this.lines.add(this.textField.text)
            cpos++
            if (this.textField.cursorPosition < this.textField.text.length) {
                this.lines[cpos + scroll - 1] = this.textField.text.substring(0..this.textField.cursorPosition)
                this.textField.text = this.textField.text.substring(this.textField.cursorPosition)
                this.textField.cursorPosition = 0
            } else {
                this.textField.text = ""
            }
        }
    }

    private fun moveLine(up: Boolean = true){
        if(this.textField.text.isNotBlank())
            this.lines[cpos + scroll] = this.textField.text
        if(up) cpos-- else cpos++
        if(cpos + scroll < this.lines.size){
            this.textField.text = this.lines[cpos + scroll]
            this.lines[cpos + scroll] = ""
        }
    }

    fun onDraw() {
        Gui.drawRect(this.x, this.y, this.w, this.h, Color.BLACK.rgb)
        this.textField.drawTextBox()
        val shownLines = if(this.lines.size > this.linesCap) {
            lines.subList(this.scrollTop, this.scrollBottom)
        }else{
            lines
        }
        shownLines.withIndex().forEach{
            val (i, s) = it
            this.fontRenderer.drawString(s, this.cx, cy + (i * 8), Color.WHITE.rgb)
        }
        val linesstr = "Lines: ${this.lines.size}"
        val cursorstr = "Cursor Pos: ${this.textField.cursorPosition}, ${this.cpos}"
        val scrollstr = "Scroll: ${this.scroll}"
        val filenamestr = "File: ${this.currentFileName}"
        this.fontRenderer.drawString(linesstr, this.x + 5, this.h - 10, Color.WHITE.rgb)
        this.fontRenderer.drawString(cursorstr, (this.w / 2) - this.fontRenderer.getStringWidth(cursorstr) / 3, this.h - 10, Color.WHITE.rgb)
        this.fontRenderer.drawString(scrollstr, this.w - this.fontRenderer.getStringWidth(scrollstr) - 5, this.h - 10, Color.WHITE.rgb)
        this.fontRenderer.drawString(filenamestr, this.x + 5, 5, Color.WHITE.rgb)
//        this.textField.drawDebugBox()
    }

    fun updateScreen() {
        this.textField.updateCursorCounter()
        this.textField.y = cy + (8 * cpos)
    }
}

class TextEditorPackage(system: CouchDesktopSystem) : RenderablePackage(system){
    private val textEditor: TextEditor by lazy{ TextEditor(system, this) }
    override val renderer: AbstractSystemScreen by lazy{ GuiTextEditor(system, textEditor) }
    override val name: String
        get() = "mcte"
    override val version: String
        get() = "0.1"

    override fun init(args: Array<String>) {
        super.init(args)
        if(args.size != 1){
            return
        }
        textEditor.start(args[0])
    }

    override fun onUpdate() {
        textEditor.update()
    }
}

class GuiTextEditor(system: CouchDesktopSystem, val textEditor: TextEditor) : AbstractSystemScreen(system){
    override val x: Int = 20
    override val y: Int = 20

    val scrollableTextField by lazy{
        ScrollableTextField(this.fontRenderer, this)
    }

    override fun onInit() {
        this.w = this.width - x
        this.h = this.height - y
        scrollableTextField.init(this.x, this.y, this.w, this.h)
    }

    fun saveFile(data: String){
        val prepareData = {
            val nbt = NBTTagCompound()
            nbt.setString("text", data)
            nbt
        }
        val processData: ProcessData = { d, _, _, _ ->
            if(d.hasKey("text")){
                val text = d.getString("text")
                this.textEditor.saveFile(text)
            }
        }
        MessageFactory.sendDataToServer("saveFile", this.system.desktop.pos, prepareData, processData)
    }

    override fun onDraw() {
        scrollableTextField.onDraw()
    }

    override fun onKeyTyped(typedChar: Char, keyCode: Int) {
        scrollableTextField.keyTyped(typedChar, keyCode)
    }

    override fun onUpdate() {
        scrollableTextField.updateScreen()
    }

}

data class TextEditorSettings(val path: String, var foregroundColor: Int, var backgroundColor: Int)

class TextEditor(val system: CouchDesktopSystem, val tepack: TextEditorPackage){
    private var currentFile: TextFile? = null
    private val fs = system.os?.fileSystem!!

    private val settings = TextEditorSettings("/home/packages/mcte/.settings", Color.WHITE.rgb, Color.BLACK.rgb)

    fun openFile(name: String){
        if(fs.doesFileExist(name, true)){
            val file = fs.getFile(name, true) ?: return
            if(file.fileType == FileTypes.TEXT.type){
                this.currentFile = file as TextFile
                syncWithGui()
            }
        }else{
            currentFile = TextFile(name)
            syncWithGui()
        }
    }

    fun exit(){
        this.tepack.exit()
    }

    fun saveFile(data: String){
        this.currentFile?.writeData(data)
    }

    fun syncWithGui(){
        val prepareData: () -> NBTTagCompound = {
            this.currentFile?.data ?: NBTTagCompound()
        }
        val processData: ProcessData = { data, _, _, _ ->
            val screen = Minecraft.getMinecraft().currentScreen
            if(screen is GuiTextEditor){
                if(data.hasKey("text")){
                    val text = data.getTagList("text", TAG_STRING)
                    text.forEach {
                        val str = (it as NBTTagString).string
                        screen.scrollableTextField.lines += str
                    }
                }
                screen.scrollableTextField.currentFileName = data.getString("name")
            }
        }
        MessageFactory.sendDataToClient("syncWithGui", this.system.player as EntityPlayerMP, this.system.desktop.pos, prepareData, processData)
    }

    fun start(fileName: String){
        if(!fs.doesFileExist(settings.path, false)){
            fs.makeFile(settings.path, "text"){
                val nbt = NBTTagCompound()
                nbt.setInteger("fore_col", this.settings.foregroundColor)
                nbt.setInteger("back_col", this.settings.backgroundColor)
                nbt
            }
        }

        this.openFile(fileName)
    }

    fun update(){}
}