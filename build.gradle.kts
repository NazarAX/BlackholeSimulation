plugins {
    application
    java
}

repositories {
    mavenCentral()
}

val lwjglVersion = "3.3.3"

val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()

val lwjglNatives = when {
    osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm64")) -> "natives-macos-arm64"
    osName.contains("mac") -> "natives-macos"
    osName.contains("win") -> "natives-windows"
    else -> "natives-linux"
}

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-opengl")

    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")
}

application {
    mainClass.set("com.nazarii.BlackholeSimulator")
    if (osName.contains("mac")) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
    }
}