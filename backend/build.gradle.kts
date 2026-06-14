plugins {
    id("java")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile> {
    options.release = 21
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.4")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jsoup:jsoup:1.18.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mybatis.spring.boot:mybatis-spring-boot-starter-test:3.0.4")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register<JavaExec>("debugScrape") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "org.example.debug.ScraperDebugger"
    args = listOf(project.findProperty("url")?.toString() ?: "https://www.grawewohnen.at/")
}

tasks.register<JavaExec>("populateCache") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "org.example.debug.PopulateCache"
}
