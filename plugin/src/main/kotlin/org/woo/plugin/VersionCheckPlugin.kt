package org.woo.plugin

import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging

class VersionCheckPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val logger = Logging.getLogger(VersionCheckPlugin::class.java)

        project.tasks.register("checkModuleVersion") {
            it.group = "verification"
            it.description = "Checks if module version is up-to-date."

            it.doLast {
                val currentVersion = project.version.toString()
                val latestVersion = fetchLatestVersion(project.group.toString(), project.name)

                if (latestVersion == null) {
                    logger.warn("❗ 최신 버전을 조회할 수 없습니다.")
                    return@doLast
                }

                if (currentVersion != latestVersion) {
                    logger.warn("⚠️ [$latestVersion] 버전이 존재합니다. 현재 사용중인 버전: [$currentVersion]")
                } else {
                    logger.lifecycle("✅ 최신 버전 [$currentVersion]을 사용 중입니다.")
                }
            }
        }
    }

    private fun fetchLatestVersion(
        groupId: String,
        artifactId: String,
    ): String? {
        val url =
            "https://maven.pkg.github.com/PARKPARKWOO/common-module/${groupId.replace('.', '/')}/$artifactId/maven-metadata.xml"

        val client = OkHttpClient()
        val request =
            Request
                .Builder()
                .url(url)
                .header("Authorization", "Bearer ${System.getenv("GITHUB_TOKEN")}")
                .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val xml = response.body?.string() ?: return null
            Regex("<latest>(.+)</latest>")
                .find(xml)
                ?.groups
                ?.get(1)
                ?.value
        }
    }
}
