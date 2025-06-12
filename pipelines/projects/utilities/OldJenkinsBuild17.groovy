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
    BatchJDK17: [
      label: "java17 && build",
      // 这里的平台名称关联发布的打包文件名称
      platform: "java",

      build: [
        when: 'java17_gradle7',
        options: bitbucketNotification,
        gradle:[
          // TODO. 执行项目构建工具的指令，执行自定义gradle Task
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

      // 将构建和测试通过的项目打包并发送到指定平台
      publish: [
        when: 'java17_gradle7',
        propertiesFile:'DEMO_Batch/gradle.properties',
        artifactPrefix:'DEMO_Batch/build/distributions/${appName}-${PACKAGE_VERSION}',
        artifactSuffix: "zip"
      ],

      // 发布前端C++项目
      publishFront: [
        artifactPrefix: 'DEMO-${PACKAGE_VERSION}',
        artifactSuffix: "zip",

        additionalArtifacts:  [
           [
              target: "scripts",
              file: 'SQL-${PACKAGE_VERSION}.zip',
           ],
           [
              target: "scripts-migration",
              file: 'SQL-MIGRATION-${PACKAGE_VERSION}.zip',
           ],
        ],

        downstream: [
           ShadockerManifestUpdate: [
               when: '^(release)/',
               job: {
                   description 'Trigger Shadoker Manifesto Update job'
                   publishers {
                       downstreamParameterized {
                           trigger('Deployment/shadoker-manifest-update') {
                               condition 'SUCCESS'
                               parameters {
                                   predefinedProp('MANIFESTS',
                                           'docker//DatabaseOracle19Manifest.yaml, ' +
                                           'docker//DatabaseOracle12Manifest.yaml, ' +
                                           'docker//DatabaseSqlServerManifest.yaml')
                                   predefinedProp('COMMIT', 'TRUE')
                               }
                           }
                       }
                   }
               },
           ],
        ],
      ],
    ],
  ]
]
