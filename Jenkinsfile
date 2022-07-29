// 加载共享库
@Library("mylib@main") _

// 导入库

import org.devops.Checkout
import org.devops.Build
import org.devops.Notice
import org.devops.UnitTest

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

    stages {
        stage("Checkout") {
            steps {
                script {
                    println("Checkout")
                    checkout.GetCode("${env.srcUrl}", "${env.branchName}")
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

        stage("Checkout") {
            steps {
                script {
                    println("Checkout")
                }
            }
        }

        post {
            always {
                script {
                    notice.EmailNotice("${env.userEmail}", currentBuild.result)
                }
            }
        }

    }

}
