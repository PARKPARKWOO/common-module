package org.woo.plugin

import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.logging.Logging
import org.gradle.internal.cc.base.logger

class VersionCheckPlugin : Plugin<Project> {
    override fun apply(root: Project) {
        if (root != root.rootProject) return

        val logger = Logging.getLogger("VersionCheck")
        root.tasks.register("checkCommonModuleDependencies") {
            it.group = "verification"
            it.description = "Check declared org.woo common‑module versions"

            it.doLast {
                root.allprojects.forEach { proj ->
                    println("🔍 Checking project ${proj.path}")
                    val token: String =
                        it.project.findProperty("gpr.key")?.toString() ?: System.getenv("GITHUB_TOKEN")
                    // 선언된 버전 읽기
                    val declaredDeps =
                        proj.configurations
                            .flatMap { conf -> conf.dependencies }
                            .filterIsInstance<ModuleDependency>()
                            .filter { it.group == "org.woo" }
                            .map { Triple(it.group!!, it.name, it.version!!) }
                            .distinct()

                    if (declaredDeps.isEmpty()) {
                        println("   • no org.woo deps")
                        return@forEach
                    }

                    // 비교
                    declaredDeps.forEach { (g, a, declared) ->
                        val latest = fetchLatestVersion(token, g, a)
                        when {
                            latest == null ->
                                logger.lifecycle("⚠️ [${proj.path}] '$a' 최신 조회 실패")
                            declared == latest ->
                                logger.lifecycle("✅ [${proj.path}] '$a' 최신 사용중: $declared")
                            else ->
                                logger.lifecycle("⚠️ [${proj.path}] '$a' 선언=$declared, 최신=$latest")
                        }
                    }
                }
            }
        }
    }

    /** group:name 으로 고유 키 */
    private fun pairKey(dep: Dependency) = "${dep.group}:${dep.name}"

    private fun fetchLatestVersion(
        token: String,
        groupId: String,
        artifactId: String,
    ): String? {
        val path = groupId.replace('.', '/')
        val url = "https://maven.pkg.github.com/PARKPARKWOO/common-module/$path/$artifactId/maven-metadata.xml"
        val client = OkHttpClient()
        val request =
            Request
                .Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/xml")
                .build()

        return client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val xml = resp.body?.string().orEmpty()
            Regex("<latest>([^<]+)</latest>").find(xml)?.groupValues?.get(1)
                ?: Regex("<version>([^<]+)</version>")
                    .findAll(xml)
                    .map { it.groupValues[1] }
                    .lastOrNull()
        }
    }
}
