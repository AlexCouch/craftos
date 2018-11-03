package messages

import blocks.TileEntityDesktopComputer
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagString
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import DevicesPlus
import os.filesystem.*

val startOSBootMessageHandler = IMessageHandler<StartOSBootMessage, InitializeOSMessage>{ msg, ctx ->
    val mc = Minecraft.getMinecraft()
    mc.addScheduledTask {
        val player = Minecraft.getMinecraft().player
        player.openGui(DevicesPlus, 0, player.world, msg.blockpos.x, msg.blockpos.y, msg.blockpos.z)
    }
    InitializeOSMessage(msg.blockpos)
}

val initializeOSMessageHandler = IMessageHandler<InitializeOSMessage, IMessage>{ msg, ctx ->
    val mcs = ctx.serverHandler.player.server ?: return@IMessageHandler null
    mcs.addScheduledTask {
        val comp = getCurrentComputer(ctx, msg.blockpos, ctx.side)
        comp.system.os?.start()
    }
    null
}

val syncFileSystemClientMessageHandler = IMessageHandler<SyncFileSystemClientMessage, IMessage>{ msg, ctx ->
    Minecraft.getMinecraft().addScheduledTask {
        val comp = getCurrentComputer(ctx, msg.blockpos, Side.CLIENT)
        val system = comp.system
        val os = system.os ?: return@addScheduledTask
        val fsdata = msg.fsdata
        val fname = fsdata.getString("name")
        val fdata = fsdata.getCompoundTag("data")
        val currdir = Folder(fname, NBTTagCompound())
        currdir.deserialize(fdata)
        os.fileSystem.currentDirectory = currdir
    }
    null
}

val changeScreenModeMessageHandler = IMessageHandler<ChangeScreenModeMessage, IMessage>{ msg, ctx ->
    val comp = getCurrentComputer(ctx, msg.blockpos, ctx.side)
//    comp.system.os?.screen?.mode = msg.mode
    null
}

fun getCurrentComputer(ctx: MessageContext, pos: BlockPos, side: Side): TileEntityDesktopComputer {
    val world = if(side == Side.SERVER) ctx.serverHandler.player.world else Minecraft.getMinecraft().world
    val te = world.getTileEntity(pos) as TileEntityDesktopComputer
    te.player = if(side == Side.SERVER) ctx.serverHandler.player else Minecraft.getMinecraft().player
    return te
}

val saveTermHistoryInStorageHandler = IMessageHandler<SaveTermHistoryInMemory, LoadTermHistoryInStorageMessage>{ msg, ctx ->
    var term = NBTTagCompound()
    ctx.serverHandler.player.server?.addScheduledTask {
        val te = getCurrentComputer(ctx, msg.pos, Side.SERVER)
        val termHistory = NBTTagCompound()
        termHistory.setTag("terminal_history", msg.data)
        termHistory.setString("name", "terminal_history")
        val mem = te.system.memory
        term = mem.pointerTo("terminal_history")
    }
    LoadTermHistoryInStorageMessage(term, msg.pos)
}

val loadTermHistoryInStorageHandler = IMessageHandler<LoadTermHistoryInStorageMessage, IMessage>{ msg, ctx ->
    Minecraft.getMinecraft().addScheduledTask {
        val list = msg.nbt.getTagList("data", Constants.NBT.TAG_STRING)
        val history = arrayListOf<String>()
        list.forEach {
            history.add((it as NBTTagString).string)
        }
        val te = getCurrentComputer(ctx, msg.pos, Side.CLIENT)
        val system = te.system
        system.os?.screen?.loadTerminalHistory(history)
    }
    null
}

val terminalExecuteCommandMessage = IMessageHandler <TerminalExecuteCommandMessage, IMessage>{ msg, ctx ->
    val mcs = ctx.serverHandler.player.server ?: return@IMessageHandler null
    mcs.addScheduledTask {
        val te = getCurrentComputer(ctx, msg.pos, ctx.side)
        val system = te.system
        val terminal = system.os?.terminal ?: return@addScheduledTask
        if(terminal.verifyCommandOrPackage(msg.command)){
            val command = terminal.getCommand(msg.command)
            terminal.executeCommand(ctx.serverHandler.player, command, msg.args)
        }else{
            val pack = terminal.getPackage(msg.command)
            terminal.openPackage(ctx.serverHandler.player, pack, msg.args)
        }
    }
    null
}

val displayStringOnTerminalHandler = IMessageHandler<DisplayStringOnTerminal, IMessage>{ msg, ctx ->
    Minecraft.getMinecraft().addScheduledTask {
        val te = getCurrentComputer(ctx, msg.pos, Side.CLIENT)
        val system = te.system
        val os = system.os
        os?.screen?.printToScreen(msg.message)
    }
    null
}

val openTerminalGuiMessageHandler = IMessageHandler<OpenTerminalGuiMessage, IMessage>{ msg, ctx ->
    Minecraft.getMinecraft().addScheduledTask {
        val player = Minecraft.getMinecraft().player
        player.openGui(DevicesPlus, 0, Minecraft.getMinecraft().world, msg.pos.x, msg.pos.y, msg.pos.z)
    }
    null
}

val startTerminalMessageHandler = IMessageHandler<StartTerminalMessage, OpenTerminalGuiMessage>{ msg, ctx ->
    val mcs = ctx.serverHandler.player.server ?: return@IMessageHandler null
    mcs.addScheduledTask {
        val comp = getCurrentComputer(ctx, msg.blockpos, ctx.side)
        val system = comp.system
        val os = system.os ?: return@addScheduledTask
        val terminal = os.terminal
        terminal.start(ctx.serverHandler.player)
    }
    OpenTerminalGuiMessage(msg.blockpos)
}