plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.0.0-beta11"
}

group = "com.ourcraft"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // ProtocolLib（多版本数据包适配）
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    // Paper 1.21 API — 编译产物可向前兼容 1.21+ 及 26.x 服务端
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    // ProtocolLib — 跨版本数据包抽象，替代直接 NMS 调用
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
    // bStats — 通过 Shadow 打包进最终 jar
    implementation("org.bstats:bstats-bukkit:3.2.1")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") { expand("version" to project.version) }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set("OurcraftGuard")
    // 将 bStats 重定位到本插件包，避免与其他插件冲突
    relocate("org.bstats", "${project.group}.guard.libs.bstats")
    dependencies {
        // 只打包 bStats，不打包其他依赖
        exclude { it.moduleGroup != "org.bstats" }
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.build { dependsOn(tasks.shadowJar) }
