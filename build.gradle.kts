plugins {
    `java-library`
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "regalowl.simpledatalib"
            artifactId = "simpledatalib"
            version = "0.1.088-SNAPSHOT"
        }
    }
}

group = "regalowl.simpledatalib"
version = "0.1.088-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
}

dependencies {
    // Database drivers (provided at runtime)
    compileOnly("org.xerial:sqlite-jdbc:3.49.1.0")
    compileOnly("com.mysql:mysql-connector-j:9.1.0")
    
    // Connection pooling
    implementation("com.zaxxer:HikariCP:6.3.0")
    
    // YAML
    compileOnly("org.yaml:snakeyaml:2.2")
    
    // CSV
    compileOnly("com.opencsv:opencsv:5.9")
    
    // Optional JPA support
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.1.0")
    
    // Logging
    compileOnly("org.slf4j:slf4j-api:2.0.16")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

