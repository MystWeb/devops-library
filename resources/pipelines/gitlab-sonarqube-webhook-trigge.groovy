// 加载共享库
@Library("mylib@main") _

// 导入库
import org.devops.Checkout
import org.devops.CodeScan
import org.devops.GitLab
import org.devops.Notice

// New实例化
def checkout = new Checkout()
def codeScan = new CodeScan()
def gitlab = new GitLab()
def notice = new Notice()

pipeline {
    agent { label "build" }

    options {
        skipDefaultCheckout true
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '180', numToKeepStr: '90')
    }

    environment {
        gitlabKeysCredentialsId = "a7d76450-d876-44a8-8d96-92f11cd013b0"
        sonarqubeUserTokenCredentialsId = "c23d40dd-a6c8-4a17-a0d1-23dd795fe773"
        sonarqubeHostUrl = "http://192.168.100.150:9000/sonarqube"
        dingTalkTokenCredentialsId = "8c6083c7-e1c2-47c0-9367-b67a9469bcd5"
        dingTalkRebotIdCredentialsId = "7a711aa6-04b5-4a6d-9907-c0f6b90f6acc"
        gitlabUserTokenCredentialsId = "36e10c3d-997d-4eaa-9e46-d9848d5d6631"
        srcUrl = "${env.gitlabSourceRepoURL}"
        buName = "${env.gitlabSourceNamespace}"
        commitId = "${env.gitlabMergeRequestLastCommit}"
    }

    stages {
        stage("Global") {
            steps {
                script {
                    // DEBUG Print Jenkins All Env
                    // sh 'printenv'
                    env.repoName = env.gitlabSourceRepoURL?.tokenize('/')?.last()?.replace('.git', '')
                    env.branchName = env.gitlabBranch ?: env.gitlabTargetBranch
                    env.projectKey = "${env.buName}-${repoName}"
                    // 获取项目ID和Commit ID
                    env.commitShortId = "${env.commitId}".substring(0, 8)
                    env.sonarReportUrl = "${env.sonarqubeHostUrl}/dashboard?id=${env.projectKey}&branch=${env.branchName}"
                    env.projectId = gitlab.GetProjectId("${env.gitlabUserTokenCredentialsId}", "${env.buName}", "${env.repoName}")
                    // 服务版本号（推荐定义："${branchName}-${commitId}"）
                    env.version = "${env.branchName}-${env.commitShortId}"
                    currentBuild.displayName = "${env.version}"
                    currentBuild.description = """<br>
                    🔹 项目: ${env.projectKey} <br>
                    🔹 分支: ${env.branchName} <br>
                    🔹 Commit: ${env.commitShortId} <br>
                    🔹 [🔍 SonarQube 分析报告](${env.sonarReportUrl})
                    """.stripIndent()
                }
            }
        }

        stage("CodeScan") {
            steps {
                script {
                    // 创建工作目录并执行代码扫描
                    sh "[ -d ${projectId} ] || mkdir ${projectId}"
                    ws("${WORKSPACE}/${projectId}") {
                        checkout.GetCode("${env.srcUrl}", "${env.branchName}", "${env.gitlabKeysCredentialsId}")

                        codeScan.CodeScan_Sonar("${env.sonarqubeUserTokenCredentialsId}",
                                "${env.gitlabUserTokenCredentialsId}",
                                "${env.branchName}", "${env.commitId}", "${env.projectId}")

                        codeScan.SonarQubeMetricsAndNotify("${env.sonarqubeHostUrl}", "${env.projectKey}",
                                "${env.branchName}", "${env.sonarqubeUserTokenCredentialsId}",
                                "${env.dingTalkRebotIdCredentialsId}")
                    }
                }
            }
        }

        stage('Check Quality Gate') {
            steps {
                script {
                    timeout(time: 5, unit: 'MINUTES') {
                        def qg = waitForQualityGate()
                        if (qg.status == 'OK') {
                            updateGitlabCommitStatus name: 'sonarqube', state: 'success', description: 'SonarQube Quality Gate passed'
                        } else {
                            updateGitlabCommitStatus name: 'sonarqube', state: 'failed', description: 'SonarQube Quality Gate failed'
                            error "❌ 代码质量检查未通过: ${qg.status}"
                        }
                    }
                }
            }
        }

    }

    post {
        always {
            cleanWs()
        }
        success {
            script {
                notice.dingTalkPluginNotice("${env.dingTalkRebotIdCredentialsId}")
            }
        }
        failure {
            script {
                updateGitlabCommitStatus name: 'sonarqube', state: 'failed', description: 'SonarQube Quality Gate failed'
                notice.dingTalkPluginNotice("${env.dingTalkRebotIdCredentialsId}")
            }
        }
    }
}
