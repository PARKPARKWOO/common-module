package org.woo.passport.error

class PassportException(override val message: String) : RuntimeException(message)