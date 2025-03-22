package org.woo.coroutinefeign.invoke

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.BeanDefinitionHolder
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.woo.coroutinefeign.annotation.CoroutineFeignClient
import org.woo.coroutinefeign.factory.CoroutineFeignClientFactoryBean

@Configuration
class CoroutineFeignClientRegistrar : ImportBeanDefinitionRegistrar {
    override fun registerBeanDefinitions(
        importingClassMetadata: AnnotationMetadata,
        registry: BeanDefinitionRegistry,
    ) {
        // Create a scanner to find interfaces annotated with @CoroutineFeignClient
        val scanner =
            object : ClassPathBeanDefinitionScanner(registry, true) {
                override fun doScan(vararg basePackages: String): Set<BeanDefinitionHolder> = super.doScan(*basePackages)
            }

        // Add filter to only scan interfaces with @CoroutineFeignClient
        scanner.addIncludeFilter(AnnotationTypeFilter(CoroutineFeignClient::class.java))

        // Determine base packages to scan
        val basePackages = determineBasePackages(importingClassMetadata)

        // Perform the scan and register beans
        val candidates = mutableSetOf<BeanDefinition>()
        basePackages.forEach {
            candidates.addAll(scanner.findCandidateComponents(it))
        }
        candidates.forEach { candidate ->
            if (candidate.beanClassName != null) {
                val beanDefinition =
                    BeanDefinitionBuilder
                        .genericBeanDefinition(CoroutineFeignClientFactoryBean::class.java)
                        .addConstructorArgValue(Class.forName(candidate.beanClassName))
                        .getBeanDefinition()

                beanDefinition.scope = AbstractBeanDefinition.SCOPE_SINGLETON

                val beanName =
                    BeanDefinitionReaderUtils.generateBeanName(
                        beanDefinition,
                        registry,
                    )

                registry.registerBeanDefinition(beanName, beanDefinition)
            }
        }
    }

    private fun determineBasePackages(metadata: AnnotationMetadata): List<String> {
        // Get the package of the class annotated with @EnableCoroutineFeign
        val mainPackage = metadata.className.substringBeforeLast('.')
        return listOf(
            mainPackage,
            "com.park.animal",
            "org.woo.coroutinefeign",
        )
    }
}
