// 加载共享库
@Library("mylib@main") _

// 导入库
import org.devops.*

// New实例化
def checkout = new Checkout()
def build = new Build()
def notice = new Notice()
def unitTest = new UnitTest()
def custom = new Custom()
def codeScan = new CodeScan()
def gitlab = new GitLab()
def artifact = new Artifact()

// 流水线
pipeline {
    agent { label "build" }

    options {
        skipDefaultCheckout true
    }

    parameters {
        string defaultValue: 'http://192.168.20.197/devops/devops-maven-service.git', description: '仓库地址', name: 'srcUrl'
        string defaultValue: 'main', description: '分支名称', name: 'branchName'
        string defaultValue: 'f0b54c03-789d-4ca4-847d-29f83236ef8a', description: '访问凭据-GitLab', name: 'credentialsId'
        choice choices: ['maven', 'custom', 'mavenSkip', 'gradle', 'ant', 'go', 'npm', 'yarn'], description: '构建类型', name: 'buildType'
        string defaultValue: '', name: 'customBuild', description: '自定义构建命令（示例：mvn clean package -Dpmd.skip=true -Dcheckstyle.skip=true -DskipTests && mvn test）'
        choice choices: ['false', 'true'], description: '是否跳过代码扫描', name: 'skipSonar'
    }

    stages {
        stage("Checkout") {
            steps {
                script {
                    println("Checkout")
                    checkout.GetCode("${env.srcUrl}", "${env.branchName}", "${env.credentialsId}")
                }
            }
        }

        stage("Global") {
            steps {
                script {
                    // 任务名称截取构建类型（任务名称示例：devops-maven-service）
//                    env.buildType = "${JOB_NAME}".split("-")[1]
                    // Git提交ID
                    env.commitId = gitlab.GetShortCommitId()
                    // JOB任务前缀（业务名称/组名称）
                    env.buName = "${JOB_NAME}".split('-')[0]
                    env.serviceName = "${JOB_NAME}".split('_')[0]
                    // 修改Jenkins构建描述
                    currentBuild.description = """branchName：${env.branchName} \n"""
                    // 修改Jenkins构建名称
                    currentBuild.displayName = "${env.commitId}"
                }
            }
        }

        stage("Build") {
            steps {
                script {
                    println("Build")
                    if (null == "${env.customBuild}" || "${env.customBuild}".trim().length() <= 0) {
                        build.CodeBuild("${env.buildType}")
                    } else {
                        custom.CustomCommands("${env.customBuild}")
                    }
                }
            }
        }

        stage("UnitTest") {
            steps {
                script {
                    if ("${env.buildType}" == "custom") {
                        println("构建类型为：custom，跳过UnitTest阶段，如需单元测试请使用符号：&& 拼接命令")
                    } else {
                        println("UnitTest")
                        unitTest.CodeTest("${env.buildType}")
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
                    // sonar-init
                    codeScan.InitQualityProfiles("java", "${env.serviceName}", "${env.buName}")
                    // commit-status
                    projectId = gitlab.GetProjectId("${env.buName}", "${env.serviceName}")
                    // 代码扫描
                    codeScan.CodeScan_Sonar("${env.branchName}", env.commitId, projectId)
                }
            }
        }

        // 上传制品（Format：raw）
        stage("PushArtifact") {
            steps {
                script {
                    // Dir：/buName/serviceName/branch-version/serviceName-version.suffix
                    // target/demo-0.0.1-SNAPSHOT.jar
                    jarName = sh returnStdout: true, script: 'ls target | grep -E "jar\$"'
                    fileName = jarName - "\n"
                    version = "${env.branchName}-${env.commitId}"
                    fileSuffix = fileName.split('\\.')[-1]
                    newFileName = "${serviceName}-${version}.${fileSuffix}"
                    // 重命名制品文件
                    sh "cd target ; mv ${fileName} ${newFileName}"
                    // 上传制品
                    artifact.PushArtifactByApi("${env.buName}/${env.serviceName}/${version}", "target", newFileName)
                }
            }
        }

    }

}