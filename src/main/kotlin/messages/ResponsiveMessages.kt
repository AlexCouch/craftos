package messages

import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTUtil
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class ResponsiveServerMessage() : ResponsiveSidedMessage(){
    constructor(responsiveDataPacket: ResponsiveDataPacket, pos: BlockPos) : this(){
        this.dataPacket = responsiveDataPacket
        this.pos = pos
    }

    override fun fromBytes(buf: ByteBuf) {
        val d = ByteBufUtils.readTag(buf) ?: NBTTagCompound()
        if(d.hasKey("packet") && d.hasKey("pos")){
            val packet = d.getCompoundTag("packet")
            val postag = d.getCompoundTag("pos")
            this.pos = NBTUtil.getPosFromTag(postag)
            this.data = packet
            this.dataPacket = CommonDataSpace.retrieveResponsiveDataPacket("responsive_server_message") ?: return
        }
    }

    override fun toBytes(buf: ByteBuf) {
        val d = NBTTagCompound()
        d.setTag("packet", this.dataPacket?.prepareMessageData?.invoke() ?: NBTTagCompound())
        d.setTag("pos", NBTUtil.createPosTag(this.pos))
        ByteBufUtils.writeTag(buf, d)
        CommonDataSpace.storeResponsiveDataPackets("responsive_server_message", this.dataPacket ?: return)
    }

}

class ResponsiveServerMessageHandler : ResponsiveSidedMessageHandler<ResponsiveServerMessage, ClientSideMessage>(){
    override fun onMessage(message: ResponsiveServerMessage, ctx: MessageContext): ClientSideMessage? {
        val player = ctx.serverHandler.player
        val world = player.serverWorld
        val data = message.data
        val dataPacket = message.dataPacket ?: return null
        val pos = message.pos
        world.addScheduledTask {
            dataPacket.processMessageData(data, world, pos, player)
        }
        val respPacket = DataPacket(dataPacket.prepareResponseData, dataPacket.processResponseData)
        return ClientSideMessage(respPacket, pos)
    }

}

class ResponsiveClientMessage() : ResponsiveSidedMessage(){
    constructor(data: ResponsiveDataPacket, pos: BlockPos) : this(){
        this.dataPacket = data
        this.pos = pos
    }

    override fun fromBytes(buf: ByteBuf) {
        val d = ByteBufUtils.readTag(buf) ?: NBTTagCompound()
        if(d.hasKey("packet") && d.hasKey("pos")){
            val packet = d.getCompoundTag("packet")
            val postag = d.getCompoundTag("pos")
            this.pos = NBTUtil.getPosFromTag(postag)
            this.data = packet
            this.dataPacket = CommonDataSpace.retrieveResponsiveDataPacket("client_responsive_data") ?: return
        }
    }

    override fun toBytes(buf: ByteBuf) {
        val d = NBTTagCompound()
        d.setTag("packet", this.dataPacket?.prepareMessageData?.invoke() ?: NBTTagCompound())
        d.setTag("pos", NBTUtil.createPosTag(this.pos))
        ByteBufUtils.writeTag(buf, d)
        CommonDataSpace.storeResponsiveDataPackets("client_responsive_data", this.dataPacket ?: return)
    }

}

class ResponsiveClientMessageHandler : ResponsiveSidedMessageHandler<ResponsiveClientMessage, ServerSideMessage>(){
    override fun onMessage(message: ResponsiveClientMessage, ctx: MessageContext): ServerSideMessage? {
        val mc = Minecraft.getMinecraft()
        val world = mc.world
        val player = mc.player
        val data = message.data
        val pos = message.pos
        mc.addScheduledTask {
            message.dataPacket?.processMessageData?.invoke(data, world, pos, player)
        }
        val dataPacket = DataPacket(
                message.dataPacket?.prepareResponseData ?: return null,
                message.dataPacket?.processResponseData ?: return null
        )
        return ServerSideMessage(dataPacket, pos)
    }

}