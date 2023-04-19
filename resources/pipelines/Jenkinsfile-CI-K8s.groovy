// 加载共享库
@Library("mylib@main") _

// 导入库

// New实例化
def checkout = new Checkout()
def build = new Build()
def notice = new Notice()
def unitTest = new UnitTest()
def custom = new Custom()
def codeScan = new CodeScan()
def gitlab = new GitLab()
def artifact = new Artifact()
def docker = new Docker()
def kubernetes = new Kubernetes()
def projectCustom = new ProjectCustom()

// 流水线
pipeline {
    agent { label "build" }

    options {
        skipDefaultCheckout true
        buildDiscarder logRotator(daysToKeepStr: '30', numToKeepStr: '30')
    }

    parameters {
        string defaultValue: 'http://192.168.100.150/devops/devops-service.git', description: '仓库地址', name: 'srcUrl'
        string defaultValue: 'RELEASE-1.2.0', description: '分支名称', name: 'branchName'
        choice choices: ['maven', 'custom', 'mavenSkip', 'gradle', 'ant', 'go', 'npm', 'yarn'], description: '构建类型', name: 'buildType'
        string defaultValue: '', name: 'customBuild', description: '自定义构建命令（示例：mvn clean package -Dpmd.skip=true -Dcheckstyle.skip=true -DskipTests && mvn test）'
        choice choices: ['true', 'false'], description: '是否跳过代码扫描', name: 'skipSonar'
        choice choices: ['true', 'false'], description: '是否跳过单元测试', name: 'skipTests'
    }

    environment {
        // GitLab用户密钥访问凭据Id：id_ed25519 (GitLab-Enterprise-私钥文件（192.168.100.150:/root/.ssh/id_ed25519）)
        gitlabKeysCredentialsId = "7f714471-562a-4ddd-884b-186f90556a9a"
        // 制品仓库地址
        artifactRegistry = "192.168.100.150:8081"
        // 制品仓库访问凭据Id：Nexus-admin-账号密码（192.168.100.150:8081）
        artifactCredentialsId = "adfe55cc-1f4a-444a-9c9f-7fc635c46a3c"
        // 制品仓库名称
        artifactRepository = "devops-artifacts"
        // 镜像仓库地址
        imageRegistry = "192.168.100.150:8082"
        // 镜像仓库访问凭据Id：Harbor-admin-账号密码（192.168.100.150:8082）
        imageRegistryCredentialsId = "cc81ccc9-962f-42ab-bbe6-fa9383c6938f"
        // SonarQube访问凭据Id：SonarQube-admin-token（192.168.100.150:9000）
        sonarqubeUserTokenCredentialsId = "c23d40dd-a6c8-4a17-a0d1-23dd795fe773"
        // DingTalk-robot-token（Jenkins钉钉群聊）
        dingTalkTokenCredentialsId = "8c6083c7-e1c2-47c0-9367-b67a9469bcd5"
        // DingTalk-robot-id（Jenkins钉钉群聊）
        dingTalkRebotIdCredentialsId = "5213e392-d78e-4a9a-a37e-91f394309df1"
        // GitLab用户Token访问凭据Id：GitLab-DevOps-token（Your_GitLab_Enterprise_Edition_URL，users：devops）
        gitlabUserTokenCredentialsId = "36e10c3d-997d-4eaa-9e46-d9848d5d6631"
    }

    stages {
        stage("Checkout") {
            steps {
                script {
                    println("Checkout")
                    checkout.GetCode("${params.srcUrl}", "${params.branchName}", "${env.gitlabKeysCredentialsId}")
                }
            }
        }

        stage("Global") {
            steps {
                script {
                    // 任务名称截取构建类型（任务名称示例：devops-maven-service）
//                    params.buildType = "${JOB_NAME}".split("-")[1]
                    // Git提交ID
                    env.commitId = gitlab.GetShortCommitIdByEightDigit()
                    // JOB任务前缀（业务名称/组名称）
                    env.buName = "${JOB_NAME}".split('-')[0]
                    // 服务/项目名称
                    env.serviceName = "${JOB_NAME}".split('_')[0]
                    // 服务版本号（推荐定义："${branchName}-${commitId}"）
                    env.version = "${params.branchName}-${env.commitId}"

                    // Git项目Id
                    env.projectId = projectCustom.getProjectIdByProjectName("${env.serviceName}")
                    if ("${env.projectId}" == "null") {
                        env.projectId = gitlab.GetProjectId("${env.gitlabUserTokenCredentialsId}", "${env.buName}", "${env.serviceName}")
                    }
                    println("projectId：${env.projectId}")

                    // 修改Jenkins构建描述
                    currentBuild.description = """ branchName：${params.branchName} \n commitId：${env.commitId} """
                    // 修改Jenkins构建名称
                    currentBuild.displayName = "${env.version}"
                }
            }
        }

        stage("Build") {
            steps {
                script {
                    println("Build")
                    if (null == "${params.customBuild}" || "${params.customBuild}".trim().length() <= 0) {
                        build.CodeBuild("${params.buildType}")
                    } else {
                        custom.CustomCommands("${params.customBuild}")
                    }
                }
            }
        }

        stage("UnitTest") {
            when {
                environment name: 'skipTests', value: 'false'
            }
            steps {
                script {
                    if ("${params.buildType}" == "custom") {
                        println("构建类型为：custom，跳过UnitTest阶段，如需单元测试请使用符号：&& 拼接命令")
                    } else {
                        println("UnitTest")
                        unitTest.CodeTest("${params.buildType}")
                    }
                }
            }
        }

        stage("CodeScan") {
            when {
                environment name: 'skipSonar', value: 'false'
            }
            steps {
                script {
                    println("CodeScan")
                    // 代码扫描 commit-status
                    codeScan.CodeScan_Sonar("${env.sonarqubeUserTokenCredentialsId}", "${env.gitlabUserTokenCredentialsId}",
                            "${params.branchName}", "${env.commitId}", "${env.projectId}")
                }
            }
        }

        // 上传制品（Format：raw）
        stage("PushArtifact") {
            steps {
                script {
                    // Dir：/buName/serviceName/branch-version/serviceName-version.suffix
                    // target/demo-0.0.1-SNAPSHOT.jar
                    if ("${env.buildType}" == "maven" || "${env.buildType}" == "mavenSkip") {
                        env.filePath = "target"
                        fileSuffix = "jar"
                        env.newFileName = "${env.serviceName}-${env.version}.${fileSuffix}"
                        // fileSuffix = env.fileName.split('\\.' as Closure)[-1]
                        originalFileName = sh returnStdout: true, script: "ls ${env.filePath} | grep -E ${fileSuffix}\$"
                        originalFileName = "${originalFileName}" - "\n"
                        // 重命名制品文件
                        sh "cd ${env.filePath} ; mv ${originalFileName} ${env.newFileName}"
                        // 上传制品
                        artifact.PushArtifactByApi("${env.artifactRegistry}", "${env.artifactCredentialsId}", "${env.artifactRepository}",
                                "${env.buName}/${env.serviceName}/${env.version}", "${env.buildType}", "${env.filePath}", "${env.newFileName}")
                    } else if ("${env.buildType}" == "npm" || "${env.buildType}" == "yarn") {
                        env.filePath = "dist"
                        fileSuffix = "tar.gz"
                        env.newFileName = "${env.serviceName}-${env.version}.${fileSuffix}"
                        sh """
                            cd ${env.filePath} && tar -zcvf ${env.newFileName} *
                        """
                        // 上传制品
                        artifact.PushArtifactByApi("${env.artifactRegistry}", "${env.artifactCredentialsId}", "${env.artifactRepository}",
                                "${env.buName}/${env.serviceName}/${env.version}", "${env.buildType}", "${env.filePath}", "${env.newFileName}")
                    } else {
                        env.result = sh returnStdout: true, script: "sh artifact.sh ${env.filePath} ${env.serviceName} ${env.version}" - "\n"
                        env.newFileName = "${env.result}" - "\n"
                        println("通过项目内自定义脚本上传制品")
                    }
                }
            }
        }

        stage("DockerBuild") {
            steps {
                script {
                    // imageTag："${params.branchName}-${env.commitId}"
                    env.imageName = "${env.buName}/${env.serviceName}"
                    docker.DockerBuildAndPushImage("${env.imageRegistry}", "${env.imageRegistryCredentialsId}",
                            "${env.imageName}", "${env.version}", "${env.filePath}", "${env.newFileName}")
                }
            }
        }

        stage("K8sReleaseFile") {
            steps {
                script {
                    // Git项目Id（devops-k8s-deployment）
                    k8sProjectId = gitlab.GetProjectId("${env.gitlabUserTokenCredentialsId}", "devops", "devops-k8s-deployment")
                    // Git文件模板名称
                    fileName = "k8s-deployments-template.yaml"
                    // Git上传文件路径：项目服务名称/版本号.yaml
                    filePath = "${env.serviceName}%2fNative%2f${env.version}.yaml"
                    // 下载Kubernetes部署模板文件
                    fileData = gitlab.GetRepositoryFile("${env.gitlabUserTokenCredentialsId}", "${k8sProjectId}",
                            "${fileName}", "main")
                    // imagePath：镜像仓库地址/镜像名称:镜像标签
                    imagePath = "${env.imageRegistry}/${env.imageName}:${env.version}"
                    // Kubernetes发布模板文件内容替换并转换Base64
                    base64Content = kubernetes.K8sReleaseTemplateFileReplaceAndConvertToBase64("${fileName}", "${fileData}", "${imagePath}")

                    // 上传替换后的版本文件（新建文件或者更新文件）
                    // gitlab文件内容变更（URL编码转义符： %2f = / ）
                    try {
                        gitlab.CreateRepositoryFile("${env.gitlabUserTokenCredentialsId}", "${k8sProjectId}", "main", "${filePath}", "${base64Content}")
                    } catch (e) {
                        gitlab.UpdateRepositoryFile("${env.gitlabUserTokenCredentialsId}", "${k8sProjectId}", "main", "${filePath}", "${base64Content}")
                    }
                }
            }
        }

    }

    post {
        always {
            // clean workspace build
            cleanWs()
        }
        success {
            script {
//                notice.dingTalkNotice("${env.dingTalkTokenCredentialsId}")
                notice.dingTalkPluginNotice("${env.dingTalkRebotIdCredentialsId}")
            }
        }
        failure {
            script {
//                notice.dingTalkNotice("${env.dingTalkTokenCredentialsId}")
                notice.dingTalkPluginNotice("${env.dingTalkRebotIdCredentialsId}")
            }
        }
    }

}