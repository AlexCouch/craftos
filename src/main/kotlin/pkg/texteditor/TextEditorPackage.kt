package pkg.texteditor

import pkg.*
import client.AbstractSystemScreen
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import os.filesystem.File
import system.CouchDesktopSystem
import java.awt.Color

class ScrollableTextField(
        private val fontRenderer: FontRenderer,
        private val x: Int,
        private val y: Int,
        private val w: Int,
        private val h: Int
) : Gui(){
    private var cpos = 0
        set(np){
            field = np
            when {
                field <= 0 -> field = 0
                field >= this.lines.size -> field = this.lines.size - 1
            }
        }

    private var cx = x + 5
    private var cy = y + 5

    private val textField by lazy{
        GuiTextField(
                0,
                fontRenderer,
                cx,
                cy,
                w - 5,
                25
        )
    }

    private val linesCap by lazy { this.h / this.textField.height }

    private val lines = arrayListOf<String>()
    private var shownLines = listOf<String>()
    private var scroll: Int = 0
        set(s){
            if(field < 0) field = 0
            if(field >= this.lines.size) field = this.lines.size - 1
            field = s
        }

    fun init(){
        this.textField.isFocused = true
        this.textField.enableBackgroundDrawing = false
        this.lines.add("")
    }

    fun keyTyped(typedChar: Char, keyCode: Int){
        when(keyCode){
            Keyboard.KEY_UP -> {
                moveLine()
            }
            Keyboard.KEY_DOWN -> {
                moveLine(false)
            }
            Keyboard.KEY_RETURN -> {
                if(cpos < this.lines.size)
                    this.lines.add(cpos, this.textField.text)
                else
                    this.lines.add(this.textField.text)
                cpos++
                if(this.textField.cursorPosition < this.textField.text.length){
                    this.lines[cpos - 1] = this.textField.text.substring(0..this.textField.cursorPosition)
                    this.textField.text = this.textField.text.substring(this.textField.cursorPosition)
                    this.textField.cursorPosition = 0
                }else{
                    this.textField.text = ""
                }
            }
            Keyboard.KEY_BACK -> {
                when {
                    this.textField.text.isBlank() -> {
                        if(cpos > 0) {
                            this.textField.text = this.lines[cpos]
                            cpos--
                            this.lines.removeAt(cpos)
                            this.textField.setCursorPositionEnd()
                        }
                    }
                    this.textField.cursorPosition == 0 -> {
                        val currLine = this.textField.text
                        val prevLine = this.lines[cpos-1]
                        val merged = prevLine + currLine
                        if(merged.length > this.textField.maxStringLength){
                            val cutBefore = merged.substring(0, this.textField.maxStringLength)
                            val cutAfter = merged.substring(this.textField.maxStringLength)
                            cpos--
                            this.lines[cpos] = ""
                            this.textField.text = cutBefore
                            this.lines[cpos+1] = cutAfter
                            this.textField.cursorPosition = prevLine.length
                        }else{
                            this.lines.removeAt(cpos - 1)
                            this.textField.text = merged
                            cpos--
                            this.textField.cursorPosition = prevLine.length
                        }
                    }
                    else -> this.textField.textboxKeyTyped(typedChar, keyCode)
                }
            }
            else -> {
                if(this.textField.cursorPosition == this.textField.maxStringLength){
                    moveLine(false)
                }
                this.textField.textboxKeyTyped(typedChar, keyCode)
            }
        }
    }

    private fun moveLine(up: Boolean = true){
        if(this.textField.text.isNotBlank())
            this.lines[cpos] = this.textField.text
        if(up) cpos-- else cpos++
        if(cpos < this.lines.size){
            this.textField.text = this.lines[cpos]
            this.lines[cpos] = ""
        }
    }

    fun onDraw() {
        this.textField.drawTextBox()
        if(this.shownLines.size > this.linesCap) {
            this.shownLines = lines.subList(this.y * scroll, linesCap)
        }else{
            this.shownLines = lines
        }
        shownLines.withIndex().forEach{
            val (i, s) = it
            if(i < this.lines.size-this.linesCap){
                return
            }
            this.fontRenderer.drawString(s, this.cx, cy + (i * 8), Color.WHITE.rgb)
        }
        val linesstr = "Lines: ${this.lines.size}"
        this.fontRenderer.drawString(linesstr, 10, this.h - this.fontRenderer.getWordWrappedHeight(linesstr, 20), Color.WHITE.rgb)
    }

    fun updateScreen() {
        this.textField.updateCursorCounter()
        this.textField.y = cy + (8 * cpos)
    }
}

class TextEditorPackage(system: CouchDesktopSystem) : RenderablePackage(system){
    override val renderer: AbstractSystemScreen = GuiTextEditor(system)
    private val textEditor: TextEditor = TextEditor(system)
    override val name: String
        get() = "mcte"
    override val version: String
        get() = "0.1"

    override fun init() {
        super.init()
        textEditor.start()
    }

    override fun onUpdate() {
        textEditor.update()
    }

}

class GuiTextEditor(system: CouchDesktopSystem) : AbstractSystemScreen(system){
    override val x: Int = 20
    override val y: Int = 20


    private val scrollableTextField by lazy{
        ScrollableTextField(
                this.fontRenderer,
                this.x,
                this.y,
                this.w,
                this.h
        )
    }

    override fun onInit() {
        this.w = this.width - x
        this.h = this.height - y
        scrollableTextField.init()
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

class TextEditor(val system: CouchDesktopSystem){
    private val currentFile: File? = null
    private val fs = system.os?.fileSystem!!

    fun openFile(name: String){
        if(fs.currentDirectory.files.stream().anyMatch { it.name == name }){

        }
    }

    fun start(){
    }

    fun update(){

    }
}