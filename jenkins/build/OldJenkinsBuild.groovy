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

  // 配置项目构建的不同阶段: build > test > sonar > publish
  buildTargets: [
    BatchJDK17: [
      label: "java17 && build",
      platform: "java",

      build: [
        when: 'java17_gradle7',
        options: bitbucketNotification,
        gradle:[
          cmd: '-PappVersion=${PACKAGE_VERSION} buildPackage -x test',
          dir: 'DEMO_Batch',
        ],
      ],
      unitTest :  [
        when: 'java17_gradle7',
        enabled: true,
        label: "java17",
        gradle:[
          cmd: 'cleanTest test jacocoTestReport',
          dir: 'DEMO_Batch',
        ],
        testResultsDirectory: 'DEMO_Batch/DEMO_Core/build/test-results/test',
        jacocoReport: false,
        jUnitPattern: '*.xml',
      ],
      publish: [
        when: 'java17_gradle7',
        propertiesFile:'DEMO_Batch/gradle.properties',
        artifactPrefix:'DEMO_Batch/build/distributions/${appName}-${PACKAGE_VERSION}',
        artifactSuffix: "zip"
      ],
    ],

    BatchJDK8: [
      label: "build && java8.191",
      platform: "java",

      build: [
        when : 'develop|release',
        options: bitbucketNotification,
        gradle:[
          cmd: '-PappVersion=${PACKAGE_VERSION} buildPackage -x test',
          dir: 'DEMO_Batch',
        ],
      ],
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
