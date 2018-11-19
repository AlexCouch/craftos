package messages

import io.netty.buffer.ByteBuf
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTUtil
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.ByteBufUtils
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class ServerSideMessage() : BasicSidedMessage(){

    constructor(name: String, dataPacket: DataPacket, pos: BlockPos) : this(){
        this.dataPacket = dataPacket
        this.pos = pos
        this.name = name
    }

    override fun fromBytes(buf: ByteBuf) {
        val d = ByteBufUtils.readTag(buf) ?: NBTTagCompound()
        if(d.hasKey("packet") && d.hasKey("pos") && d.hasKey("name")){
            val name = d.getString("name")
            val packet = d.getCompoundTag("packet")
            val postag = d.getCompoundTag("pos")
            this.pos = NBTUtil.getPosFromTag(postag)
            this.data = packet
            this.dataPacket = CommonDataSpace.retrieveDataPacket(name) ?: return
        }
    }

    override fun toBytes(buf: ByteBuf) {
        val d = NBTTagCompound()
        d.setString("name", this.name)
        d.setTag("packet", this.dataPacket?.prepareMessageData?.invoke() ?: NBTTagCompound())
        d.setTag("pos", NBTUtil.createPosTag(this.pos))
        ByteBufUtils.writeTag(buf, d)
        CommonDataSpace.storeDataPackets(this.name, this.dataPacket ?: return)
    }

}

class ServerSideMessageHandler : BasicSidedMessageHandler<ServerSideMessage>(){
    override fun onMessage(message: ServerSideMessage, ctx: MessageContext): IMessage? {
        val player = ctx.serverHandler.player
        val world = player.serverWorld
        world.addScheduledTask {
            message.dataPacket?.processMessageData?.invoke(message.data, world, message.pos, player)
        }
        return null
    }

}