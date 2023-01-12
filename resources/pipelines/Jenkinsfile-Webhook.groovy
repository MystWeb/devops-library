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

/**
 * Post content parameters
 * - Variable = webhookData
 * - Expression = $
 * - Variable = object_kind
 * - Expression = $.object_kind
 * - Variable = before
 * - Expression = $.before
 * - Variable = after
 * - Expression = $.after
 * - Token = ${GitLab-ProjectName}
 *
 * admin/application_settings/network/出站请求/允许来自 web hooks 和服务对本地网络的请求 = true
 * ${GitLab-ProjectName}/Settings/Webhooks
 * - URL = http://JENKINS_URL/generic-webhook-trigger/invoke?token=${GitLab-ProjectName}
 * - SSL验证 = true
 */
// 打印WebHook变量值
println("Object Kind：${object_kind} \n Before：${before} \n After：${after}")
println("Git Lab WebhookData：${webhookData}")

// 使用readJSON工具解析JSON数据
webhookData = readJSON text: "${webhookData}"

// 全局变量
env.srcUrl = webhookData.project.git_http_url
env.branchName = webhookData.ref - "refs/heads/"
env.commitId = webhookData.checkout_sha[0..7] // 前8位的commitId
env.credentialsId = "f0b54c03-789d-4ca4-847d-29f83236ef8a"
env.userEmail = webhookData.user_email

// 修改Jenkins构建描述
currentBuild.description = """
srcUrl：${env.srcUrl} \n
branchName：${env.branchName} \n
"""
// 修改Jenkins构建名称
currentBuild.displayName = "${env.commitId}"

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

    post {
        always {
            script {
                println("邮件通知")
                notice.EmailNotice("${env.userEmail}", currentBuild.result)
            }
        }
    }

}