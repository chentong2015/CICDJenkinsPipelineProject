class GroovyScript {

    // Groovy会自动进行类型推断
    // Groovy代码可以不用行末尾的分号
    static main( args) {
        new GroovyScript().run()
    }

    def run() {
        final String value = "foobar"
        println value
        new Helper().hello()
    }

    // 等效于Java定义类型的方式
    class Helper {
        def hello() {
            println "hello helper"
        }
    }

    static def appendSummary(String testName, value) {
        println(testName)
    }

    def invokeMaven(projectDir, options) {
        sh "settings.sh && mvn -f " + projectDir + "/pom.xml $MVN_ARGS " + options
    }

    // 获取执行Job构建的系统类型
    static def getOsType() {
        String os = System.properties['os.name']
        if (os != null ) {
            if (os.toLowerCase().contains('windows')) {
                os = 'Windows'
            } else if (os.toLowerCase().contains('linux')) {
                os = "Linux"
            } else {
                os = "Mac"
            }
        }
        return ['OS': os]
    }
}