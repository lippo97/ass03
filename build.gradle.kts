plugins {
    java
    application
}

repositories {
	mavenCentral()
}

dependencies {
    implementation("io.vertx:vertx-core:4.3.4")
    implementation("io.vertx:vertx-web:4.3.4")
    implementation("org.javatuples:javatuples:1.2")
	implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0-rc2")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.14.0-rc2")
}

application {
    mainClass.set("pcd.ass03.raft.Main")
}
