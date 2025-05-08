plugins {
    id("com.gradleup.shadow") version "8.3.6"
    id("java")
}

group = "de.bypixeltv"
version = "1.1.1-Beta"

repositories {
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }

    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://repo.opencollab.dev/main/")
    }

    maven {
        url = uri("https://repo.vulpescloud.de/snapshots")
    }

}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    // Jedis and SnakeYAML
    implementation("redis.clients:jedis:6.0.0")
    implementation("org.yaml:snakeyaml:2.4")

    // CommandAPI
    implementation("dev.jorel:commandapi-velocity-shade:10.0.1")

    implementation("org.json:json:20250107")

    // Lombok dependencies
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    compileOnly("org.projectlombok:lombok:1.18.38")

    // CloudNet
    val cloudNetVersion = "4.0.0-RC11.1"
    compileOnly("eu.cloudnetservice.cloudnet:driver:$cloudNetVersion")
    compileOnly("eu.cloudnetservice.cloudnet:bridge:$cloudNetVersion")
    compileOnly("eu.cloudnetservice.cloudnet:wrapper-jvm:$cloudNetVersion")

    compileOnly("org.geysermc.geyser:api:2.4.2-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")

    val vulpesCloudVersion = "1.1.0"
    compileOnly("de.vulpescloud", "VulpesCloud-wrapper", vulpesCloudVersion)
}

sourceSets {
    getByName("main") {
        java {
            srcDir("src/main/java")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(23)
    }

    shadowJar {
        archiveBaseName.set("RediVelocity")
        archiveVersion.set(version.toString())
        archiveClassifier.set("")

        relocate("redis.clients", "de.bypixeltv.shaded.redis.clients")
        relocate("org.yaml.snakeyaml", "de.bypixeltv.shaded.org.yaml.snakeyaml")
        relocate("dev.jorel.commandapi", "de.bypixeltv.shaded.dev.jorel.commandapi")
        relocate("org.json", "de.bypixeltv.shaded.org.json")
    }

    build {
        dependsOn(shadowJar)
    }
}
