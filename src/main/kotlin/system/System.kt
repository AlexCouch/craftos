package system

import blocks.TileEntityDesktopComputer
import net.minecraftforge.fml.common.network.NetworkRegistry
import os.OperatingSystem

object DeviceSystem{
    var os: OperatingSystem? = null
    val systemStream = NetworkRegistry.INSTANCE.newSimpleChannel("system")
    fun start(te: TileEntityDesktopComputer){
        os = te.os
    }
}