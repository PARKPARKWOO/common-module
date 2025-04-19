package org.woo.plugin

import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.logging.Logging
import org.gradle.internal.cc.base.logger

class VersionCheckPlugin : Plugin<Project> {
    override fun apply(root: Project) {
        if (root != root.rootProject) return

        val logger = Logging.getLogger("VersionCheck")

        root.tasks.register("checkCommonModuleDependencies") {
            it.group = "verification"
            it.description = "Check org.woo common‑module usage via resolutionResult"
            val token: String =
                it.project.findProperty("gpr.key")?.toString() ?: System.getenv("GITHUB_TOKEN")
            it.doLast {
                // 루트 + 서브프로젝트 모두 검사
                (listOf(root) + root.subprojects).forEach { proj ->
                    println("🔍 Checking project ${proj.path}")

                    // 1) 모든 resolvable configuration 에 대해 resolutionResult 의존성 가져오기
                    val deps =
                        proj.configurations
                            .filter { it.isCanBeResolved }
                            .flatMap { conf ->
                                conf.incoming.resolutionResult.allDependencies
                            }.filterIsInstance<ResolvedDependencyResult>()
                            .mapNotNull { dep ->
                                val id = dep.selected.id

                                when (id) {
                                    // 프로젝트 간 의존성
                                    is ProjectComponentIdentifier -> {
                                        // id.projectPath -> ":domain-auth" 등
                                        val p = root.findProject(id.projectPath) ?: return@mapNotNull null
                                        if (p.group == "org.woo") {
                                            Triple(p.group.toString(), p.name, p.version.toString())
                                        } else {
                                            null
                                        }
                                    }

                                    // 외부 모듈 (Maven) 의존성
                                    is ModuleComponentIdentifier -> {
                                        if (id.group == "org.woo") {
                                            Triple(id.group, id.module, id.version)
                                        } else {
                                            null
                                        }
                                    }

                                    else -> null
                                }
                            }.distinct() // 중복 제거

                    if (deps.isEmpty()) {
                        println("   • no org.woo deps in ${proj.path}")
                        return@forEach
                    }

                    // 2) 버전 비교
                    deps.forEach { (g, a, declared) ->
                        val latest = fetchLatestVersion(token, g, a)
                        if (latest == null) {
                            logger.warn("⚠️ [${proj.path}] '$a' 최신버전 조회 실패")
                        } else if (declared != latest) {
                            logger.warn("⚠️ [${proj.path}] '$a' 선언=$declared, 최신=$latest")
                        } else {
                            logger.lifecycle("✅ [${proj.path}] '$a' 최신 사용중: $declared")
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
