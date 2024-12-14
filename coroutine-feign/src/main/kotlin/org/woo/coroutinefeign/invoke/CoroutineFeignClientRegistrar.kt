package org.woo.coroutinefeign.invoke

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.woo.coroutinefeign.annotation.CoroutineFeignClient

@Configuration
class CoroutineFeignClientRegistrar : ImportBeanDefinitionRegistrar {
    override fun registerBeanDefinitions(
        importingClassMetadata: AnnotationMetadata,
        registry: BeanDefinitionRegistry,
    ) {
        // Scan for interfaces with @CoroutineFeignClient annotation
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AnnotationTypeFilter(CoroutineFeignClient::class.java))

        // Define base packages to scan (customize as needed)
        val basePackages = listOf("com.park.animal", "org.woo")

        basePackages.forEach { basePackage ->
            scanner.findCandidateComponents(basePackage).forEach { beanDefinition ->
                val beanClassName = beanDefinition.beanClassName
                val beanClass = Class.forName(beanClassName)

                // Register the interface as a bean
                val definition = GenericBeanDefinition()
                definition.setBeanClass(beanClass)
                definition.scope = BeanDefinition.SCOPE_SINGLETON

                registry.registerBeanDefinition(beanClass.simpleName, definition)
            }
        }
    }
}
