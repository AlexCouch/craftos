package pkg.texteditor

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.ResourceLocation

//This just removes the mouse crap so that you can't click. I mean, min linux distro installs don't have mouse input
class MouselessTextField(
        id: Int,
        fontRenderer: FontRenderer,
        x: Int,
        y: Int,
        w: Int,
        h: Int
) : GuiTextField(
        id,
        fontRenderer,
        x,
        y,
        w,
        h
){
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean = false
}