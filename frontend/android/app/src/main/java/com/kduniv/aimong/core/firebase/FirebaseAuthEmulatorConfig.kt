package com.kduniv.aimong.core.firebase

data class FirebaseAuthEmulatorConfig(
    val enabled: Boolean,
    val host: String = HOST,
    val port: Int = PORT
) {
    companion object {
        private const val HOST = "10.0.2.2"
        private const val PORT = 9099

        fun create(isDebugBuild: Boolean): FirebaseAuthEmulatorConfig =
            FirebaseAuthEmulatorConfig(enabled = isDebugBuild)
    }
}
