plugins {
    id("io.spring.dependency-management")
    kotlin("jvm")
}
java.sourceCompatibility = JavaVersion.VERSION_21

dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.1") }
}

dependencies {
    implementation(project(":polls-domain"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("org.apache.pdfbox:pdfbox:3.0.4")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations { compileOnly { extendsFrom(configurations.annotationProcessor.get()) } }
tasks.withType<Test> { useJUnitPlatform() }
