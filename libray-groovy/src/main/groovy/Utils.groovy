class Utils {

    def appendSummary(String testName, value) {
        println(testName)
    }

    def invokeMaven(projectDir, options) {
        sh "settings.sh && mvn -f " + projectDir + "/pom.xml $MVN_ARGS " + options
    }
}