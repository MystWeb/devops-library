// 加载共享库
@Library("mylib@main") _

// 导入库
import org.devops.*

// New实例化
def checkout = new Checkout()
def build = new Build()
def notice = new Notice()
def unitTest = new UnitTest()

// 任务名称截取构建类型（任务名称示例：devops-maven-service）
env.buildType = "${JOB_NAME}".split("-")[1]

// 流水线
pipeline {
    agent { label "build" }

    options {
        skipDefaultCheckout true
    }

	parameters {
  		string defaultValue: 'http://192.168.20.191/devops/devops-maven-service.git', description: '仓库地址', name: 'srcUrl'
  		string defaultValue: 'main', description: '分支名称', name: 'branchName'
  		string defaultValue: 'f0b54c03-789d-4ca4-847d-29f83236ef8a', description: '访问凭据', name: 'credentialsId'
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
                    build.CodeBuild("${env.buildType}")
//                    sh "${env.buildShell}"
                }
            }
        }

        stage("UnitTest") {
            steps {
                script {
                    println("UnitTest")
                    unitTest.CodeTest("${env.buildType}")
                }
            }
        }

    }

}