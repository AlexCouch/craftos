package blocks

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
import DevicesPlus
import net.minecraft.block.ITileEntityProvider
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTException
import net.minecraft.world.IBlockAccess
import terminal.TerminalResponse

object DesktopComputerBlock : Block(Material.IRON), ITileEntityProvider {
    var beginStartup = false

    var te: TileEntityDesktopComputer? = null
    init{
        val name = "desktop"
        this.unlocalizedName = name
        this.registryName = ResourceLocation(modid, name)
        this.setCreativeTab(CreativeTabs.MISC)
    }

    override fun onBlockAdded(worldIn: World, pos: BlockPos, state: IBlockState) {
        super.onBlockAdded(worldIn, pos, state)
        te = TileEntityDesktopComputer(this)
    }

    override fun onBlockActivated(worldIn: World, pos: BlockPos, state: IBlockState, playerIn: EntityPlayer, hand: EnumHand, facing: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean {
        if(worldIn.isRemote){
            this.te?.startup(playerIn) //Calls the tile entity function 'startup" which is supposed to start the operating system and display a screen
        }
        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ)
    }

    override fun createTileEntity(world: World?, state: IBlockState?): TileEntity = te ?: TileEntityDesktopComputer(this)
    override fun createNewTileEntity(worldIn: World?, meta: Int): TileEntity? = te

    override fun isFullCube(state: IBlockState?): Boolean = false
    override fun isOpaqueCube(state: IBlockState?): Boolean = false
}


class TileEntityDesktopComputer(val block: DesktopComputerBlock) : TileEntity(){
    var commandResponse: TerminalResponse? = null
    lateinit var os: OperatingSystem
    val storage = NBTTagCompound()

    fun startup(player: EntityPlayer){
        println("Starting system...")
        player.openGui(DevicesPlus, 0, this.world, this.pos.x, this.pos.y, this.pos.z)
    }

    fun postCommandResponse(response: TerminalResponse){
        commandResponse = response
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

    override fun writeToNBT(compound: NBTTagCompound): NBTTagCompound {
        super.writeToNBT(compound)
        compound.setTag("storage", storage)
        return compound
    }
}
