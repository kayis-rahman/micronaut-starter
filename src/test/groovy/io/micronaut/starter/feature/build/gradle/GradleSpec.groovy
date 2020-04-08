package io.micronaut.starter.feature.build.gradle

import io.micronaut.context.BeanContext
import io.micronaut.starter.feature.build.gradle.templates.annotationProcessors
import io.micronaut.starter.feature.build.gradle.templates.buildGradle
import io.micronaut.starter.feature.build.gradle.templates.gradleProperties
import io.micronaut.starter.feature.build.gradle.templates.settingsGradle
import io.micronaut.starter.feature.graalvm.GraalNativeImage
import io.micronaut.starter.feature.jdbc.Dbcp
import io.micronaut.starter.feature.jdbc.Hikari
import io.micronaut.starter.feature.jdbc.Tomcat
import io.micronaut.starter.feature.micrometer.MicrometerFeature
import io.micronaut.starter.feature.server.Jetty
import io.micronaut.starter.feature.server.Netty
import io.micronaut.starter.feature.server.Undertow
import io.micronaut.starter.fixture.FeatureFixture
import io.micronaut.starter.fixture.ProjectFixture
import io.micronaut.starter.options.Language
import io.micronaut.starter.options.TestFramework
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class GradleSpec extends Specification implements ProjectFixture, FeatureFixture {

    @Shared
    @AutoCleanup
    BeanContext beanContext = BeanContext.run()

    void "test settings.gradle"() {
        String template = settingsGradle.template(buildProject()).render().toString()

        expect:
        template.contains('rootProject.name="foo"')
    }

    void "test gradle.properties"() {
        String template = gradleProperties.template([name: "Sally", age: "30"]).render().toString()

        expect:
        template.contains('name=Sally')
        template.contains('age=30')
    }

    void "test annotation processor dependencies"() {
        when:
        String template = annotationProcessors.template(buildWithFeatures(Language.java)).render().toString()

        then:
        template.contains('annotationProcessor platform("io.micronaut:micronaut-bom:\$micronautVersion")')
        template.contains('annotationProcessor "io.micronaut:micronaut-inject-java"')
        template.contains('annotationProcessor "io.micronaut:micronaut-validation"')

        when:
        template = annotationProcessors.template(buildWithFeatures(Language.kotlin)).render().toString()

        then:
        template.contains('kapt platform("io.micronaut:micronaut-bom:\$micronautVersion")')
        template.contains('kapt "io.micronaut:micronaut-inject-java"')
        template.contains('kapt "io.micronaut:micronaut-validation"')

        when:
        template = annotationProcessors.template(buildWithFeatures(Language.groovy)).render().toString()

        then:
        template.contains('compileOnly platform("io.micronaut:micronaut-bom:\$micronautVersion")')
        template.contains('compileOnly "io.micronaut:micronaut-inject-groovy"')
    }

    void "test junit with different languages"() {
        when:
        String template = buildGradle.template(buildProject(), buildWithFeatures(Language.java)).render().toString()

        then:
        template.contains("""
    testAnnotationProcessor platform("io.micronaut:micronaut-bom:\$micronautVersion")
    testAnnotationProcessor "io.micronaut:micronaut-inject-java"
    testImplementation platform("io.micronaut:micronaut-bom:\$micronautVersion")
    testImplementation "org.junit.jupiter:junit-jupiter-api"
    testImplementation "io.micronaut.test:micronaut-test-junit5"
    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine"
""")

        when:
        template = buildGradle.template(buildProject(), buildWithFeatures(Language.groovy, TestFramework.junit)).render().toString()

        then:
        template.contains("""
    testImplementation platform("io.micronaut:micronaut-bom:\$micronautVersion")
    testImplementation "io.micronaut:micronaut-inject-groovy"
    testImplementation "org.junit.jupiter:junit-jupiter-api"
    testImplementation "io.micronaut.test:micronaut-test-junit5"
    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine"
""")
        !template.contains("testAnnotationProcessor")

        when:
        template = buildGradle.template(buildProject(), buildWithFeatures(Language.kotlin, TestFramework.junit)).render().toString()

        then:
        template.contains("""
    kaptTest platform("io.micronaut:micronaut-bom:\$micronautVersion")
    kaptTest "io.micronaut:micronaut-inject-java"
    testImplementation platform("io.micronaut:micronaut-bom:\$micronautVersion")
    testImplementation "org.junit.jupiter:junit-jupiter-api"
    testImplementation "io.micronaut.test:micronaut-test-junit5"
    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine"
""")
        !template.contains("testAnnotationProcessor")
    }

    void 'test graal-native-image feature'() {
        when:
        String template = buildGradle.template(buildProject(), buildWithFeatures(Language.java, new GraalNativeImage())).render().toString()

        then:
        template.contains('annotationProcessor platform("io.micronaut:micronaut-bom:\$micronautVersion")')
        template.contains('annotationProcessor "io.micronaut:micronaut-graal"')
        template.contains('compileOnly platform("io.micronaut:micronaut-bom:\$micronautVersion")')
        template.contains('compileOnly "org.graalvm.nativeimage:svm"')

        when:
        template = buildGradle.template(buildProject(), buildWithFeatures(Language.kotlin, new GraalNativeImage())).render().toString()

        then:
        template.contains('kapt platform("io.micronaut:micronaut-bom:\$micronautVersion")')
        template.contains('kapt "io.micronaut:micronaut-graal"')
        template.contains('compileOnly platform("io.micronaut:micronaut-bom:\$micronautVersion")')
        template.contains('compileOnly "org.graalvm.nativeimage:svm"')

        when:
        template = buildGradle.template(buildProject(), buildWithFeatures(Language.groovy, new GraalNativeImage())).render().toString()

        then:
        template.count('compileOnly platform("io.micronaut:micronaut-bom:\$micronautVersion")') == 1
        template.contains('compileOnly "org.graalvm.nativeimage:svm"')
    }

    @Unroll
    void 'test jdbc feature #jdbcFeature.name'() {
        when:
        String template = buildGradle.template(buildProject(), buildWithFeatures(Language.java, jdbcFeature)).render().toString()

        then:
        template.contains("implementation \"io.micronaut.configuration:micronaut-${jdbcFeature.name}\"")

        when:
        template = buildGradle.template(buildProject(), buildWithFeatures(Language.kotlin, jdbcFeature)).render().toString()

        then:
        template.contains("implementation \"io.micronaut.configuration:micronaut-${jdbcFeature.name}\"")

        when:
        template = buildGradle.template(buildProject(), buildWithFeatures(Language.groovy, jdbcFeature)).render().toString()

        then:
        template.contains("implementation \"io.micronaut.configuration:micronaut-${jdbcFeature.name}\"")

        where:
        jdbcFeature << [new Dbcp(), new Hikari(), new Tomcat()]
    }

    @Unroll
    void 'test micrometer feature #micrometerFeature.name'() {
        given:
        String dependency = "micronaut-micrometer-registry-${micrometerFeature.name - 'micrometer-'}"

        when:
        String template = buildGradle.template(buildProject(), buildWithFeatures(Language.java, micrometerFeature)).render().toString()

        then:
        template.contains("implementation \"io.micronaut.configuration:${dependency}\"")

        when:
        template = buildGradle.template(buildProject(), buildWithFeatures(Language.kotlin, micrometerFeature)).render().toString()

        then:
        template.contains("implementation \"io.micronaut.configuration:${dependency}\"")

        when:
        template = buildGradle.template(buildProject(), buildWithFeatures(Language.groovy, micrometerFeature)).render().toString()

        then:
        template.contains("implementation \"io.micronaut.configuration:${dependency}\"")

        where:
        micrometerFeature << beanContext.getBeansOfType(MicrometerFeature).iterator()
    }

    @Unroll
    void 'test server feature #serverFeature.name'() {
        when:
        String template = buildGradle.template(buildProject(), buildWithFeatures(Language.java, serverFeature)).render().toString()

        then:
        template.contains(dependency)

        when:
        template = buildGradle.template(buildProject(), buildWithFeatures(Language.kotlin, serverFeature)).render().toString()

        then:
        template.contains(dependency)

        when:
        template = buildGradle.template(buildProject(), buildWithFeatures(Language.groovy, serverFeature)).render().toString()

        then:
        template.contains(dependency)

        where:
        serverFeature                                    | dependency
        new Netty()                                      | 'implementation "io.micronaut:micronaut-http-server-netty"'
        new Jetty()                                      | 'implementation "io.micronaut.servlet:micronaut-http-server-jetty"'
        new io.micronaut.starter.feature.server.Tomcat() | 'implementation "io.micronaut.servlet:micronaut-http-server-tomcat"'
        new Undertow()                                   | 'implementation "io.micronaut.servlet:micronaut-http-server-undertow"'
    }

}