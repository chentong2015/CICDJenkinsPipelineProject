class Packager {

    // to do some inits
    def init() {
    }

    // 判断打包需要生成的目录路径是否存在
    def pack() {
        init()
        if (new File(context.source + '/output/software').exists()) {
            ant.copy(todir: context.target + '/software', overwrite: true) {
                fileset(dir: context.source + '/output/software') {
                    include(name: '**')
                }
            }
        } else {
            throw new IOException('Could not find software directory')
        }
    }
}
