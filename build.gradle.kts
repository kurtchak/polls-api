plugins {
    id("org.springframework.boot") version "3.4.1" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "2.1.0" apply false
    kotlin("plugin.spring") version "2.1.0" apply false
    kotlin("plugin.jpa") version "2.1.0" apply false
}

subprojects {
    group = "sk.blackbell"
    version = "0.0.1-SNAPSHOT"
    repositories { mavenCentral() }
}
