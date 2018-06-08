import blocks.DesktopComputerBlock
import blocks.TileEntityDesktopComputer
import client.GuiRegistry
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.event.FMLServerStartingEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.relauncher.Side
import terminal.ClearCommand
import terminal.EchoCommand
import terminal.TerminalStream
import terminal.messages.*
import terminal.registerTerminalCommand

const val modid = "devices+"
const val name = "Devices Plus"
const val version = "0.1"

@Mod(modid=modid, name=name, version=version, modLanguageAdapter = "net.shadowfacts.forgelin.KotlinAdapter")
object DevicesPlus{

    @SidedProxy(clientSide = "ClientProxy", serverSide = "CommonProxy")
    lateinit var proxy: CommonProxy

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent){
        NetworkRegistry.INSTANCE.registerGuiHandler(this, GuiRegistry)
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent){
        GameRegistry.registerTileEntity(TileEntityDesktopComputer::class.java, "desktop")
        registerNetworking()
    }

    @Mod.EventHandler
    fun serverStarting(event: FMLServerStartingEvent){
        registerTerminalCommand(EchoCommand, ClearCommand)
    }
}

fun registerNetworking(){
    TerminalStream.streamNetwork.registerMessage(terminalExecuteCommandMessage, TerminalExecuteCommandMessage::class.java, 0, Side.SERVER)
    TerminalStream.streamNetwork.registerMessage(terminalCommandResponseToClient, TerminalCommandResponseToClient::class.java, 1, Side.CLIENT)
    TerminalStream.streamNetwork.registerMessage(saveTermHistoryInStorageHandler, SaveTermHistoryInMemory::class.java, 2, Side.SERVER)
    TerminalStream.streamNetwork.registerMessage(loadTermHistoryInStorageHandler, LoadTermHistoryInStorageMessage::class.java, 3, Side.CLIENT)
}

@Mod.EventBusSubscriber(modid=modid)
object EventHandler{
    @JvmStatic
    @SubscribeEvent
    fun registerBlocks(event: RegistryEvent.Register<Block>){
        event.registry.registerAll(DesktopComputerBlock)
    }

    @JvmStatic
    @SubscribeEvent
    fun rgisterItems(event: RegistryEvent.Register<Item>){
        val itemblock = ItemBlock(DesktopComputerBlock)
        itemblock.registryName = DesktopComputerBlock.registryName
        itemblock.unlocalizedName = DesktopComputerBlock.unlocalizedName
        event.registry.registerAll(itemblock)
    }
}

open class CommonProxy{
    open fun preInit(event: FMLPreInitializationEvent){

    }
}

class ClientProxy : CommonProxy(){
    override fun preInit(event: FMLPreInitializationEvent) {
        super.preInit(event)
    }
}