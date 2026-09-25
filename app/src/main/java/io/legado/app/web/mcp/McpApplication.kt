package io.legado.app.web.mcp

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.response.header
import io.ktor.server.routing.RoutingContext
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp

fun Application.configureMcp(
    allowedHosts: List<String>,
    allowedOrigins: List<String>,
    serverFactory: RoutingContext.() -> Server,
) {
    intercept(ApplicationCallPipeline.Plugins) {
        context.response.header(HttpHeaders.CacheControl, "no-store")
    }
    mcpStreamableHttp(
        path = McpAccess.PATH,
        allowedHosts = allowedHosts,
        allowedOrigins = allowedOrigins,
        block = serverFactory,
    )
}
