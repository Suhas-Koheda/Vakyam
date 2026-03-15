repositories { mavenCentral() }
configurations.create("koogConf")
dependencies { "koogConf"("ai.koog:koog-agents:+") }
tasks.register("resolveKoog") {
    doLast {
        configurations["koogConf"].resolvedConfiguration.resolvedArtifacts.forEach {
            println("Resolved: ${it.moduleVersion.id.version}")
        }
    }
}
