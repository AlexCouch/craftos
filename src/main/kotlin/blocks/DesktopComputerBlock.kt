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
import net.minecraft.block.ITileEntityProvider
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTBase
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraftforge.fml.common.FMLCommonHandler
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
            val te = worldIn.getTileEntity(pos) ?: throw IllegalStateException("No tile entity placed! Report to author!")
            if(te is TileEntityDesktopComputer){
                if (te.started) {
                    te.openGui()
                } else {
                    te.player = playerIn as EntityPlayerMP
                    te.startup()
                }
            }else{
                throw IllegalStateException("No desktop tile entity placed! What did you do??????")
            }
        }
        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ)
    }

    override fun createNewTileEntity(worldIn: World, meta: Int): TileEntity = TileEntityDesktopComputer()

    override fun isFullCube(state: IBlockState?): Boolean = false
    override fun isOpaqueCube(state: IBlockState?): Boolean = false
}

class TileEntityDesktopComputer : TileEntity(){
    var os: OperatingSystem? = null

    var storage = NBTTagCompound()
    var started = false
    var player: EntityPlayer? = null
    val system: CouchDesktopSystem
        get() = CouchDesktopSystem(this)

    fun startup(){
        if(started) return
        started = true
        started = true
        this.system.start()
        val blockstate = this.world.getBlockState(this.pos)
        this.world.notifyBlockUpdate(this.pos, blockstate, blockstate, 3)
        this.world.scheduleBlockUpdate(this.pos, this.blockType, 0, 0)
        markDirty()
    }

    override fun getUpdatePacket(): SPacketUpdateTileEntity? = SPacketUpdateTileEntity(this.pos, 3, this.updateTag)
    override fun getUpdateTag(): NBTTagCompound = this.serializeNBT()
    override fun onDataPacket(net: NetworkManager, pkt: SPacketUpdateTileEntity) {
        super.onDataPacket(net, pkt)
        handleUpdateTag(pkt.nbtCompound)
    }

    fun openGui(){
        stream.sendTo(OpenTerminalGuiMessage(this.pos), this.system.player as EntityPlayerMP)
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

    override fun deserializeNBT(nbt: NBTTagCompound) {
        super.deserializeNBT(nbt)
        if(this.started) {
            if (nbt.hasKey("system") && nbt.hasKey("storage")) {
                this.storage = nbt.getCompoundTag("storage")
                val system = nbt.getCompoundTag("system")
                this.system.deserialize(system)
            }
        }
    }

    override fun serializeNBT(): NBTTagCompound {
        val nbt = super.serializeNBT()
        if(this.started){
            nbt.setTag("system", this.system.serialize())
            nbt.setTag("storage", this.storage)
        }
        return nbt
    }
}
