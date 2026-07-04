package com.nullstorm.vpn.parser.dto

import android.content.Context
import com.nullstorm.vpn.parser.dto.entities.ProfileItem
import com.nullstorm.vpn.parser.enums.CoreResolvedType

data class CoreConfigContext(
    val context: Context,
    val guid: String,
    val isCustom: Boolean = false,
    val resolvedOutbounds: List<ResolvedOutbound> = emptyList(),
) {
    data class ResolvedOutbound(
        val tag: String,
        val profile: ProfileItem,
        val resolvedProfiles: List<ProfileItem>,
        val resolvedType: CoreResolvedType,
    )
}