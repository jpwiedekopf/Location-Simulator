package com.ispgr5.locationsimulator.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class to Store one Sound of the Pattern
 */
@Serializable
//The name for json. for example{"comp_type":"Sound","source":""}
@SerialName("Sound")
data class Sound(
    val source: String,
    var minVolume: Int,
    var maxVolume: Int,
    var minPause: Int,
    var maxPause: Int,
    val isRandom: Boolean,
) : ConfigComponent(){

    override fun copy() : Sound {
        return Sound(source,minVolume,maxVolume,minPause,maxPause,isRandom)
    }
}