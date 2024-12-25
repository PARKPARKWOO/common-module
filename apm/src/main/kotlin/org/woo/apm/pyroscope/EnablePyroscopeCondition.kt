package org.woo.apm.pyroscope

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata

class EnablePyroscopeCondition : Condition {
    override fun matches(
        context: ConditionContext,
        metadata: AnnotatedTypeMetadata,
    ): Boolean = metadata.isAnnotated(EnablePyroscope::class.java.name)
}
