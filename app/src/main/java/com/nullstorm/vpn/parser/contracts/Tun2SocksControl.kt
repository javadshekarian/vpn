package com.nullstorm.vpn.parser.contracts

interface Tun2SocksControl {
    /**
     * Starts the tun2socks process with the appropriate parameters.
     * This initializes the VPN tunnel and connects it to the SOCKS proxy.
     */
    fun startTun2Socks()

    /**
     * Stops the tun2socks process and cleans up resources.
     */
    fun stopTun2Socks()
}