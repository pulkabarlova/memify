plugins {
    id("memify.android.library")
    id("memify.android.compose")
    id("memify.android.hilt")
}

android {
    namespace = "com.codekotliners.memify.features.profile"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.database)
    implementation(projects.core.network)
    implementation(projects.core.user)
    implementation(projects.core.prefs)
    implementation(projects.core.ui)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.coil.compose)

    implementation(libs.ktor.client.core)

    implementation(libs.vkid.core)
}
