plugins {
    id("castivio.jvm.library")
}

dependencies {
    api(libs.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
