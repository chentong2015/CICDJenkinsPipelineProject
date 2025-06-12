def jUnitPlugin = [
  name: 'junit',
  allowEmptyResults: true,
  retainLongStdout: true,
]

def bitbucketNotification = [
  notifyBitbucket:false,
]

projectConfig = [
  projectName: "Sample-DEMO",
  viewName: "Sample-DEMO",
  productId: "DEMO",
  emailNotification: true,
  publishMetrics: true,
  emails: [
     "test@test.com"
  ],

  // 选择要构建的项目分支
  branches: [
    "master",
    "release",
  ],

  options: [
    usePipelineStageView: true
  ],

  // 设置获取构建项目的版本路径
  version: [
    gitUrl: 'ssh://git@dev-bitbucket.net:7999/Sample/DEMO.git'
  ],

  // 配置项目构建的不同阶段: build > unit test > sonar > publish
  buildTargets: [
    BatchJDK8: [
      label: "build && java8.191",
      platform: "java",

      build: [
        when : 'develop|release',
        options: bitbucketNotification,
        gradle:[
          // TODO. 执行项目构建工具的指令
          cmd: '-PappVersion=${PACKAGE_VERSION} buildPackage -x test',
          dir: 'DEMO_Batch',
        ],
      ],

      // Sonar Plugin需要和JDK版本兼容
      sonar: [
        when : 'develop|release',
        projectBaseDir: "DEMO_Batch/",
        forceUseASonarInstance: "Sonar Sample",
      ],

      publish: [
        when : 'develop|release',
        propertiesFile:'DEMO_Batch/gradle.properties',
        artifactPrefix:'DEMO_Batch/build/distributions/${appName}-${PACKAGE_VERSION}',
        artifactSuffix: "zip"
      ],
    ],
  ]
]
