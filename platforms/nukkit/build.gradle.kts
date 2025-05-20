plugins {
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow")
}

repositories {
    mavenCentral()
    maven {
        name = "opencollab-repository-maven-releases"
        url = uri("https://repo.opencollab.dev/maven-releases")
    }
}

// Set Java compatibility to version 21 to match the core projects
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    
    // Toolchain to ensure the right JDK is used
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // Include the base implementation from the common project
    implementation(project(":common:implementation:base"))

    // Include the Terra core API
    implementation(project(":common:api"))

    // Add the Nukkit API dependency (provided scope)
    compileOnly("cn.nukkit", "nukkit", "1.0-SNAPSHOT")

    // Add other necessary dependencies
    implementation("org.slf4j", "slf4j-api", "2.0.9")
    implementation("org.slf4j", "slf4j-simple", "2.0.9")
    implementation("commons-io", "commons-io", "2.11.0")
}

// Configure build tasks
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)  // Update the Java release to 21
}

// Configure JAR task
tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

// Configure Shadow JAR task
tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    
    // Ensure all runtime dependencies are included
    configurations = listOf(project.configurations.runtimeClasspath.get())

    // Include plugin.yml at the root
    from(sourceSets.main.get().resources.srcDirs) {
        include("plugin.yml")
    }
    
    // DO NOT relocate any Terra classes or dependencies to avoid classloading issues
    // Only relocate third-party libraries that aren't part of Terra
    relocate("org.apache.commons", "com.dfsek.terra.nukkit.lib.apache.commons")
    
    // Keep minimize() commented out to avoid removing classes that might be needed
    // minimize() 
}

// Ensure build depends on shadowJar
tasks.build {
    dependsOn(tasks.shadowJar)
}

// Add a specific task to process resources and replace variables in plugin.yml
// This ensures ${version} in plugin.yml gets replaced with the actual version
tasks.processResources {
    filesMatching("plugin.yml") {
        expand(
            "version" to project.version
        )
    }
}

// Configure tasks like processResources if necessary
// tasks.processResources {
//     // ...
// } 