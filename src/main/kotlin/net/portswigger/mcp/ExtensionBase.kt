package net.portswigger.mcp

import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.Registration
import net.portswigger.mcp.config.ConfigUi
import net.portswigger.mcp.config.McpConfig
import net.portswigger.mcp.intruder.McpPayloadGeneratorProvider
import net.portswigger.mcp.providers.ClaudeDesktopProvider
import net.portswigger.mcp.providers.CodexCliProvider
import net.portswigger.mcp.providers.CopilotCliProvider
import net.portswigger.mcp.providers.ManualProxyInstallerProvider
import net.portswigger.mcp.providers.OpenCodeProvider
import net.portswigger.mcp.providers.ProxyJarManager
import net.portswigger.mcp.shadow.ExchangeShadowStore

@Suppress("unused")
class ExtensionBase : BurpExtension {

    override fun initialize(api: MontoyaApi) {
        api.extension().setName("Burp Suite Complete MCP")

        val config = McpConfig(api.persistence().extensionData(), api.logging())
        val serverManager = KtorServerManager(api)

        val intruderPayloadRegistration: Registration =
            api.intruder().registerPayloadGeneratorProvider(McpPayloadGeneratorProvider())

        val proxyJarManager = ProxyJarManager(api.logging())

        val configUi = ConfigUi(
            config = config, providers = listOf(
                ClaudeDesktopProvider(api.logging(), proxyJarManager),
                OpenCodeProvider(api.logging(), proxyJarManager),
                CopilotCliProvider(api.logging(), proxyJarManager),
                CodexCliProvider(api.logging(), proxyJarManager),
                ManualProxyInstallerProvider(api.logging(), proxyJarManager),
            )
        )

        configUi.onEnabledToggled { enabled ->
            configUi.getConfig()

            if (enabled) {
                ExchangeShadowStore.start(api)
                serverManager.start(config) { state ->
                    configUi.updateServerState(state)
                }
            } else {
                serverManager.stop { state ->
                    configUi.updateServerState(state)
                }
                ExchangeShadowStore.stop()
            }
        }

        api.userInterface().registerSuiteTab("MCP", configUi.component)

        api.extension().registerUnloadingHandler {
            serverManager.shutdown()
            ExchangeShadowStore.stop()
            intruderPayloadRegistration.deregister()
            configUi.cleanup()
            config.cleanup()
        }

        if (config.enabled) {
            ExchangeShadowStore.start(api)
            serverManager.start(config) { state ->
                configUi.updateServerState(state)
            }
        }
    }
}