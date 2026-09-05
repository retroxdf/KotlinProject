import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.datetime)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.abtsplazita.posplazita.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "PlazitaPOS"
            packageVersion = "1.0.6"
            description = "Plazita Point of Sale System"
            copyright = "© 2026 ABTS Plazita"
            vendor = "ABTS Plazita"
            windows {
                menu = true
                shortcut = true
                iconFile.set(project.file("src/main/resources/LA_PLAZITA.ico"))
                // Agregar un upgradeUuid para que Windows reconozca actualizaciones correctamente
                upgradeUuid = "68c92a54-7243-41c3-8815-5e60d3d573f0"
                // Forzar que el instalador pueda sobreescribir versiones anteriores si es necesario
                dirChooser = true
            }
        }
    }
}