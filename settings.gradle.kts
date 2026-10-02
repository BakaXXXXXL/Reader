pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        google()
        mavenCentral()
    }
}

rootProject.name = "Reader_app"

// Include modules dynamically when their directory exists
listOf(
    ":core:model",
    ":core:common",
    ":core:database",
    ":core:datastore",
    ":core:designsystem",
    ":engine:parser",
    ":engine:parser:txt",
    ":engine:parser:epub",
    ":engine:typography",
    ":engine:render",
    ":feature:bookshelf",
    ":feature:reader",
    ":feature:settings",
    ":feature:stats",
    ":app"
).forEach { modulePath ->
    val dir = file(modulePath.removePrefix(":").replace(":", "/"))
    if (dir.exists()) {
        include(modulePath)
    }
}
