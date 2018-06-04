package client

import blocks.DesktopComputerBlock
import blocks.TileEntityDesktopComputer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import net.minecraftforge.fml.client.IModGuiFactory
import net.minecraftforge.fml.common.network.IGuiHandler
import org.lwjgl.opengl.GL11

class TerminalScreen : GuiScreen(){
    override fun initGui() {
        super.initGui()
    }

    override fun doesGuiPauseGame(): Boolean = false

    //Not sure what to do here for drawing a black screen and rendering text (I know how to render text)
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.drawScreen(mouseX, mouseY, partialTicks)
        GL11.glColor3f(0.0f, 0.0f, 0.0f)
        this.drawString(Minecraft.getMinecraft().fontRenderer, "Testing minecraft kernel gui!", 10, 5, 0xffffff)
    }
}

object GuiRegistry : IGuiHandler{
    override fun getClientGuiElement(ID: Int, player: EntityPlayer, world: World?, x: Int, y: Int, z: Int): Any? {
        if(ID == 0){
            return TerminalScreen() //This works
        }
        return null
    }

    override fun getServerGuiElement(ID: Int, player: EntityPlayer?, world: World?, x: Int, y: Int, z: Int): Any? = null

}