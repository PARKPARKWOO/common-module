package org.woo.core.transaction

interface Tx {
    fun <T> read(function: () -> T): T

    fun <T> write(function: () -> T): T

    fun <T> newTransaction(function: () -> T): T
}
