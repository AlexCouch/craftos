package messages

import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side

typealias ProcessData = (data: NBTTagCompound, world: World, pos: BlockPos, player: EntityPlayer) -> Unit

object MessageFactory{
    private val stream = NetworkRegistry.INSTANCE.newSimpleChannel("operating_system")

    init{
        var id = 0
        stream.registerMessage(ClientSideMessageHandler(), ClientSideMessage::class.java, ++id, Side.CLIENT)
        stream.registerMessage(ResponsiveClientMessageHandler(), ResponsiveClientMessage::class.java, ++id, Side.CLIENT)
        stream.registerMessage(ServerSideMessageHandler(), ServerSideMessage::class.java, ++id, Side.SERVER)
        stream.registerMessage(ResponsiveServerMessageHandler(), ResponsiveServerMessage::class.java, ++id, Side.SERVER)
    }

    fun sendDataToClient(player: EntityPlayerMP, pos: BlockPos, prepareData: () -> NBTTagCompound, processData: ProcessData){
        val dataPacket = DataPacket(prepareData, processData)
        val clientMessage = ClientSideMessage(dataPacket, pos)
        stream.sendTo(clientMessage, player)
    }

    fun sendDataToServer(pos: BlockPos, prepareData: () -> NBTTagCompound, processData: ProcessData){
        val dataPacket = DataPacket(prepareData, processData)
        val serverMessage = ServerSideMessage(dataPacket, pos)
        stream.sendToServer(serverMessage)
    }

    fun sendDataToServerWithResponse(
            pos: BlockPos,
            prepareMessageData: () -> NBTTagCompound,
            processMessageData: ProcessData,
            prepareResponseData: () -> NBTTagCompound,
            processResponseData: ProcessData
    ){
        val responsiveDataPacket = ResponsiveDataPacket(
                prepareMessageData,
                processMessageData,
                prepareResponseData,
                processResponseData
        )
        val responsiveServerMessage = ResponsiveServerMessage(responsiveDataPacket, pos)
        stream.sendToServer(responsiveServerMessage)
    }

    fun sendDataToClientWithResponse(
            pos: BlockPos,
            player: EntityPlayerMP,
            prepareMessageData: () -> NBTTagCompound,
            processMessageData: ProcessData,
            prepareResponseData: () -> NBTTagCompound,
            processResponseData: ProcessData
    ){
        val responsiveDataPacket = ResponsiveDataPacket(
                prepareMessageData,
                processMessageData,
                prepareResponseData,
                processResponseData
        )
        val responsiveClientMessage = ResponsiveClientMessage(responsiveDataPacket, pos)
        stream.sendTo(responsiveClientMessage, player)
    }
}

object CommonDataSpace{
    private val dataPackets = HashMap<String, DataPacket>()
    private val responsiveDataPackets = HashMap<String, ResponsiveDataPacket>()

    fun storeDataPackets(name: String, data: DataPacket){
        this.dataPackets += (name to data)
    }

    fun storeResponsiveDataPackets(name: String, data: ResponsiveDataPacket){
        this.responsiveDataPackets += (name to data)
    }

    fun retrieveDataPacket(name: String) = this.dataPackets.remove(name)

    fun retrieveResponsiveDataPacket(name: String) = this.responsiveDataPackets.remove(name)
}

data class DataPacket(val prepareMessageData: () -> NBTTagCompound, val processMessageData: ProcessData)

data class ResponsiveDataPacket(
        val prepareMessageData: () -> NBTTagCompound, val processMessageData: ProcessData,
        val prepareResponseData: () -> NBTTagCompound, val processResponseData: ProcessData
)

abstract class BasicSidedMessage() : IMessage{
    var dataPacket: DataPacket? = null
    var pos: BlockPos = BlockPos.ORIGIN
    var data = NBTTagCompound()
}

abstract class ResponsiveSidedMessage() : IMessage{
    var dataPacket: ResponsiveDataPacket? = null
    var pos: BlockPos = BlockPos.ORIGIN
    var data = NBTTagCompound()
}

abstract class BasicSidedMessageHandler<M : BasicSidedMessage> : IMessageHandler<M, IMessage>
abstract class ResponsiveSidedMessageHandler<M : ResponsiveSidedMessage, R : BasicSidedMessage> : IMessageHandler<M, R>