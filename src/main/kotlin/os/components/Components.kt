package os.components

import oshi.software.os.OperatingSystem
import programs.RenderCoords

abstract class OSComponent(){
    fun init(){

    }

    fun render(coords: RenderCoords, os: OperatingSystem){

    }
}

interface OSLayout{
    val components: List<OSComponent>
}