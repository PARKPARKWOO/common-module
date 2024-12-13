package org.woo.coroutinefeign.processor

import org.springframework.aot.hint.ExecutableMode
import org.springframework.aot.hint.ReflectionHints
import org.springframework.aot.hint.annotation.ReflectiveProcessor
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

class CoroutineFeignReflectiveProcessor : ReflectiveProcessor {
    override fun registerReflectionHints(
        hints: ReflectionHints,
        element: AnnotatedElement,
    ) {
        if (element is Method) {
            hints.registerMethod(element, ExecutableMode.INVOKE)
        }
    }
}
