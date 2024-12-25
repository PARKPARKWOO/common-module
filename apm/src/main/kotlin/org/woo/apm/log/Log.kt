package org.woo.apm.log

import org.slf4j.LoggerFactory

inline fun <reified T> T.log() = LoggerFactory.getLogger(T::class.java.simpleName)!!
