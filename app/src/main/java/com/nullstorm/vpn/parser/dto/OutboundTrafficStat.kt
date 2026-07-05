package com.nullstorm.vpn.parser.dto
data class OutboundTrafficStat(
    val tag: String,
    val direction: String,
    val value: Long,
)