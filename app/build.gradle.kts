plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    // This dependency is used by the application.
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("commons-cli:commons-cli:1.4")
    implementation("log4j:log4j:1.2.17")
    implementation("org.clojure:clojure:1.10.1")

    implementation("net.compartmental.code:minim:2.2.2")
    implementation("com.googlecode.soundlibs:tritonus-share:0.3.7.4")
    implementation("org.jogamp.jogl:jogl-all-main:2.3.2")
    implementation("org.jogamp.gluegen:gluegen-rt-main:2.3.2")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter("5.8.2")
            dependencies {
//                implementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
                implementation("org.junit.vintage:junit-vintage-engine:5.8.2")
            }
        }
    }
}

application {
    // Define the main class for the application.
    mainClass.set("hypernova.Hypernova")
}
