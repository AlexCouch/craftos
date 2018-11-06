package pkg.texteditor

import net.minecraft.entity.player.EntityPlayerMP
import os.OperatingSystem
import pkg.*
import DevicesPlus
import client.AbstractSystemScreen
import client.PrintableScreen
import messages.ProcessData
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.nbt.NBTTagCompound
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
            if(field < 0)
                field = 0
            if(field > this.shownLines.size - 1)
                field = this.shownLines.size - 1
            field = np
        }

    private var cx = x
    private var cy = y + (y * cpos)

    private val textField by lazy{
        GuiTextField(
                0,
                fontRenderer,
                cx,
                cy,
                w - 25,
                25
        )
    }

    private val lines = arrayListOf<String>()
    private var shownLines = listOf<String>()
    private var scroll: Int = 0
        set(s){
            if(field < 0) field = 0
            if(field > this.lines.size) field = this.lines.size - 1
            field = s
        }

    fun init(){
        lines.add("")
    }

    fun keyTyped(typedChar: Char, keyCode: Int){
        when(keyCode){
            Keyboard.KEY_UP -> {
                cpos--
            }
            Keyboard.KEY_DOWN -> {
                cpos++
            }
            Keyboard.KEY_RETURN -> {
                this.lines.add("")
                cpos++
            }
            else -> this.textField.textboxKeyTyped(typedChar, keyCode)
        }
    }

    fun onDraw() {
        this.shownLines = lines.subList(this.y + scroll, this.h - scroll)
        shownLines.withIndex().forEach{
            val (i, s) = it
            this.fontRenderer.drawString(s, this.x, i * this.textField.height, Color.WHITE.rgb)
        }
    }

    fun updateScreen() {
        this.textField.updateCursorCounter()
    }
}

class TextEditorPackage(system: CouchDesktopSystem) : RenderablePackage(system){
    override val renderer: AbstractSystemScreen = GuiTextEditor(system)
    private val textEditor: TextEditor = TextEditor(system, renderer as GuiTextEditor)
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

    private var cx = x + 10
    private var cy = y + 10

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

class TextEditor(val system: CouchDesktopSystem, private val renderer: GuiTextEditor){
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