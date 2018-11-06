package utils

import blocks.TileEntityDesktopComputer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

fun getCurrentComputer(world: World, pos: BlockPos, player: EntityPlayer): TileEntityDesktopComputer?{
    val te = world.getTileEntity(pos)
    if(te is TileEntityDesktopComputer){
        te.player = player
        return te
    }
    return null
}