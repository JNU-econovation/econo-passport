plugins {
    `java-library`
    `maven-publish`
    id("com.diffplug.spotless") version "6.25.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.github.JNU-econovation"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.2")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    // Passport JSON 직렬화 — MVC / Reactive 모든 소비자에서 사용 가능
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // PassportArgumentResolver, AuthAutoConfiguration은 Spring MVC 전용
    // compileOnly: MVC 소비자는 자체 spring-boot-starter-web 보유, Reactive 소비자는 제외
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-starter-validation")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

spotless {
    java {
        googleJavaFormat("1.17.0")
        indentWithTabs(2)
        endWithNewline()
        removeUnusedImports()
        trimTrailingWhitespace()
    }
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}

// JitPack 호환 publishing 설정
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            versionMapping {
                usage("java-api") { fromResolutionOf("runtimeClasspath") }
                usage("java-runtime") { fromResolutionResult() }
            }

            pom {
                name.set("econo-passport")
                description.set(
                    "Passport-based authentication library for ECONO microservices. " +
                        "Provides @PassportAuth annotation for Spring MVC services behind api-gateway."
                )
                url.set("https://github.com/JNU-econovation/econo-passport")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("econo-team")
                        name.set("ECONO Development Team")
                        organization.set("Econovation")
                        organizationUrl.set("https://econovation.kr")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/JNU-econovation/econo-passport.git")
                    developerConnection.set("scm:git:ssh://github.com/JNU-econovation/econo-passport.git")
                    url.set("https://github.com/JNU-econovation/econo-passport")
                }
            }
        }
    }
}
