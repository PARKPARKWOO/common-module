package org.woo.apm.pyroscope

import io.pyroscope.http.Format
import io.pyroscope.javaagent.EventType
import io.pyroscope.javaagent.PyroscopeAgent
import io.pyroscope.javaagent.config.Config.Builder
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration

@Configuration
@Conditional(EnablePyroscopeCondition::class)
class PyroscopeConfig(
    @Value("\${spring.application.name}")
    val appName: String,
    @Value("\${pyroscope.address}")
    val pyroscopeAddress: String,
) {
    init {
        fun init() {
            print("Initialize pyroscope")
            PyroscopeAgent.start(
                Builder()
                    .setApplicationName(appName)
                    .setProfilingEvent(EventType.ITIMER)
                    .setFormat(Format.JFR)
                    .setServerAddress(pyroscopeAddress)
                    .build(),
            )
        }
    }
}
