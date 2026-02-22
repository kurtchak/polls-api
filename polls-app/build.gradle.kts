import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}
java.sourceCompatibility = JavaVersion.VERSION_21

dependencies {
    implementation(project(":polls-domain"))
    implementation(project(":polls-sync"))
    implementation(project(":polls-bff"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> { useJUnitPlatform() }
tasks.withType<KotlinCompile> { compilerOptions { freeCompilerArgs.add("-Xjsr305=strict") } }
tasks.processResources { filesMatching("application.properties") { expand(project.properties) } }
