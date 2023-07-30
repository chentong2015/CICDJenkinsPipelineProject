class Main {

    static main( args) {
        new Main().run()
    }

    def run() {
        final String value = "foobar"
        println value
        new Helper().hello()
    }

    class Helper {
        def hello() {
            println "hello helper"
        }
    }
}