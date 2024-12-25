package org.woo.apm.pyroscope

import org.springframework.context.annotation.Import

@Import(PyroscopeConfig::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnablePyroscope
