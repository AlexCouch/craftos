package blocks

import com.teamwizardry.librarianlib.features.autoregister.TileRegister
import com.teamwizardry.librarianlib.features.base.block.tile.TileMod
import com.teamwizardry.librarianlib.features.base.block.tile.TileModTickable
import com.teamwizardry.librarianlib.features.saving.Savable
import com.teamwizardry.librarianlib.features.saving.Save
import modid
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import os.OperatingSystem
import net.minecraft.block.ITileEntityProvider
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTBase
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import stream
import system.CouchDesktopSystem
import terminal.messages.OpenTerminalGuiMessage

object DesktopComputerBlock : Block(Material.IRON), ITileEntityProvider {
    init{
        val name = "desktop"
        this.unlocalizedName = name
        this.registryName = ResourceLocation(modid, name)
        this.setCreativeTab(CreativeTabs.MISC)
    }

    override fun onBlockActivated(worldIn: World, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean {
        if(!worldIn.isRemote && hand == EnumHand.MAIN_HAND){
            if(playerIn is EntityPlayerMP){
                val te = worldIn.getTileEntity(pos) ?: throw IllegalStateException("No tile entity placed! Report to author!")
                if(te is TileEntityDesktopComputer){
                    if(te.started){
                        te.openGui()
                    }else{
                        te.startup(playerIn)
                    }
                }else{
                    throw IllegalStateException("No desktop tile entity placed! What did you do??????")
                }
            }
        }
        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ)
    }

    override fun createNewTileEntity(worldIn: World, meta: Int): TileEntity = TileEntityDesktopComputer()

    override fun isFullCube(state: IBlockState?): Boolean = false
    override fun isOpaqueCube(state: IBlockState?): Boolean = false
}


@Savable
@TileRegister("desktop")
class TileEntityDesktopComputer : TileMod(){
    @Save
    var os: OperatingSystem? = null

    @Save
    val storage = NBTTagCompound()
    @Save
    var system: CouchDesktopSystem? = null
    @Save
    var started = false

    fun startup(player: EntityPlayerMP){
        if(started) return
        println("Starting system...")
        system = CouchDesktopSystem(this)
        system?.player = player
        started = true
        this.system?.start()
        markDirty()
    }

    fun openGui(){
        stream.sendTo(OpenTerminalGuiMessage(this.pos), this.system?.player!!)
    }

    fun writeToStorage(name: String, nbt: NBTBase){
        if(storage.hasKey(name)){
            val tag = storage.getCompoundTag(name)
            if(tag == nbt){
                return
            }
        }
        storage.setTag(name, nbt)
    }

    fun writeOverStorageAt(at: String, nbt: NBTBase){
        storage.removeTag(at)
        writeToStorage(at, nbt)
    }

    fun readFromStorage(name: String): NBTBase{
        if(storage.hasKey(name)){
            return storage.getTag(name)
        }
        throw IllegalArgumentException("No such tag in storage: $name")
    }
}
