package network

import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.math.BlockPos
import system.CouchDesktopSystem
import messages.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class Port<T>(
        open val portId: Int,
        val t: T
){
    var available = false
    abstract fun start()
}
/*
class NetworkPort(
        override val portId: Int,
        val system: CouchDesktopSystem
) : Port<SystemNetwork>(portId, SystemNetwork(system.player as EntityPlayerMP, system)){
    override fun start(){
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate({
            this.t.scanForRouters(system.desktop.pos)
        },0, 300, TimeUnit.SECONDS)
    }

    fun connect(name: String){
        this.t.connectToRouter(system.desktop.pos, this.t.getRouterByName(name))
    }
}*/

/*
class SystemNetwork(val user: EntityPlayerMP, val system: CouchDesktopSystem){
    val routers = ArrayList<Device>()

    fun scanForRouters(pos: BlockPos){
        val world = Minecraft.getMinecraft().world
        val range = DeviceConfig.getSignalRange()

        for (y in -range until range + 1) {
            for (z in -range until range + 1) {
                for (x in -range until range + 1) {
                    val pos1 = BlockPos(pos.x + x, pos.y + y, pos.z + z)
                    val tileEntity = world.getTileEntity(pos1)
                    if (tileEntity is TileEntityRouter) {
                        routers.add(Device(tileEntity))
                    }
                }
            }
        }
    }

    fun doesRouterExist(name: String): Boolean{
        var ret = false
        routers.forEach {
            if(it.name == name) ret = true
        }
        return ret
    }

    fun getRouterByName(name: String): Router{
        if(doesRouterExist(name)){
            return (routers.filter { it.name == name }[0].getDevice(system.desktop.world) as TileEntityRouter).router
        }
        throw RuntimeException(IllegalStateException("Could not find router by name $name"))
    }

    fun connectToRouter(pos: BlockPos, router: Router){
        val connect = TaskConnect(pos, router.pos)
        connect.setCallback { t, success ->
            if(success){
                system.os?.shell?.sendMessageToClient(DisplayStringOnTerminal("Connected to router '${router.id}", pos), user)
            }
        }
        TaskManager.sendTask(connect)
    }
}*/
