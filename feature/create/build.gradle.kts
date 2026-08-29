plugins {
    id("memify.android.library")
    id("memify.android.compose")
    id("memify.android.hilt")
}

android {
    namespace = "com.codekotliners.memify.features.create"
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.feature.templates)
    implementation(projects.feature.viewer)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.coil.compose)
    implementation(libs.coil.base)
}
