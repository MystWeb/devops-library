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

// 任务名称截取构建类型（任务名称示例：devops-maven-service）
// env.buildType = "${JOB_NAME}".split("-")[1]

// 流水线
pipeline {
    agent { label "build" }

    options {
        skipDefaultCheckout true
    }

    parameters {
        string defaultValue: 'http://192.168.20.197/devops/devops-maven-service.git', description: '仓库地址', name: 'srcUrl'
        string defaultValue: 'main', description: '分支名称', name: 'branchName'
        string defaultValue: 'f0b54c03-789d-4ca4-847d-29f83236ef8a', description: '访问凭据', name: 'credentialsId'
        choice choices: ['maven', 'mavenSkip', 'gradle', 'ant', 'go', 'npm', 'yarn'], description: '构建类型', name: 'buildType'
        string defaultValue: '', description: '自定义构建命令（示例：mvn clean package -Dpmd.skip=true -Dcheckstyle.skip=true -DskipTests）', name: 'customBuild'
        string defaultValue: '', description: '自定义测试命令（示例：mvn test）', name: 'customTest'
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
                    println("UnitTest")
                    if (null == "${env.customTest}" || "${env.customTest}".trim().length() <= 0) {
                        unitTest.CodeTest("${env.buildType}")
                    } else {
                        custom.CustomCommands("${env.customTest}")
                    }
                }
            }
        }

    }

}