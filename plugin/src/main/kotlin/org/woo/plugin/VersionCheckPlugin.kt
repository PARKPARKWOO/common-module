package org.woo.plugin

import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.logging.Logging
import org.gradle.internal.cc.base.logger

class VersionCheckPlugin : Plugin<Project> {
    override fun apply(root: Project) {
        val logger = Logging.getLogger(VersionCheckPlugin::class.java)

        // 루트에만 태스크 등록
        if (root != root.rootProject) return

        root.tasks.register("checkCommonModuleDependencies") {
            it.group = "verification"
            it.description =
                "Checks all projects that depend on org.woo common‑modules and warns if they are not using the latest version."
            val token: String =
                it.project.findProperty("gpr.key")?.toString() ?: System.getenv("GITHUB_TOKEN")

            it.doLast {
                (listOf(root) + root.subprojects).forEach { sub ->
                    // 'implementation', 'api' 등 의존성 선언이 있는 모든 configuration 탐색
                    sub.configurations
                        .filter { it.isCanBeResolved }
                        .flatMap { config ->
                            config.dependencies
                                .filter { dep -> dep.group == "org.woo" }
                        }.distinctBy { dep -> pairKey(dep) }
                        .forEach { dep ->
                            val declared = dep.version
                            val latest = fetchLatestVersion(token, dep.group!!, dep.name)
                            if (latest == null) {
                                logger.warn("⚠️ [${sub.path}] '${dep.group}:${dep.name}' 최신 버전 조회 실패")
                            } else if (declared != latest) {
                                logger.warn(
                                    "⚠️ [${sub.path}] '${dep.name}' 버전 불일치: 선언된=$declared, 최신=$latest",
                                )
                            } else {
                                logger.lifecycle(
                                    "✅ [${sub.path}] '${dep.name}' 최신 버전 사용 중: $declared",
                                )
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
