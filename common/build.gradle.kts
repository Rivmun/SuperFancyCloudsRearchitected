val fabricLoaderVersion: String by extra
val archApiVersion: String by extra
val clothVersion: String by extra

dependencies {
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modApi("dev.architectury:architectury:$archApiVersion")
    modCompileOnly("me.shedaniel.cloth:cloth-config:$clothVersion") {
        exclude("net.fabricmc.fabric-api")
    }
}

architectury {
    common("fabric", "forge", "neoforge")
}

loom {
    accessWidenerPath.set(file("src/main/resources/sfcr.accesswidener"))
}
