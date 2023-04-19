// 加载共享库
@Library("mylib@main") _

// 导入库
import org.devops.GitLab
import org.devops.Kubernetes
import org.devops.ProjectCustom

// New实例化
def gitlab = new GitLab()
def kubernetes = new Kubernetes()
def projectCustom = new ProjectCustom()

pipeline {
    agent {
        label "k8s"
    }

    options {
        skipDefaultCheckout true
        buildDiscarder logRotator(daysToKeepStr: '30', numToKeepStr: '30')
    }

    parameters {
        string defaultValue: 'RELEASE-1.2.0', description: '注意：选择发布分支', name: 'branchName'
        choice choices: ['dev01', 'dev02', 'test01', 'test02'], description: '环境列表', name: 'envList'
        choice choices: ['devops.demo.ui', 'devops.demo.service'], description: '服务访问的域名（注意：域名末尾会追加环境选择：domainName.dev01）', name: 'domainName'
        string defaultValue: '8080', description: '注意：服务监听的端口号', name: 'port'
    }

    environment {
        // GitLab用户密钥访问凭据Id：id_ed25519 (GitLab-Enterprise-私钥文件（192.168.100.150:/root/.ssh/id_ed25519）)
        gitlabKeysCredentialsId = "7f714471-562a-4ddd-884b-186f90556a9a"
        // DingTalk-robot-token（Jenkins钉钉群聊）
        dingTalkTokenCredentialsId = "8c6083c7-e1c2-47c0-9367-b67a9469bcd5"
        // DingTalk-robot-id（Jenkins钉钉群聊）
        dingTalkRebotIdCredentialsId = "5213e392-d78e-4a9a-a37e-91f394309df1"
        // GitLab用户Token访问凭据Id：GitLab-DevOps-token（Your_GitLab_Enterprise_Edition_URL，users：devops）
        gitlabUserTokenCredentialsId = "36e10c3d-997d-4eaa-9e46-d9848d5d6631"
    }

    stages {
        stage("Global") {
            steps {
                script {
                    // 任务名称截取构建类型（任务名称示例：devops-maven-service）
//                    env.buildType = "${JOB_NAME}".split("-")[1]
                    // JOB任务前缀（业务名称/组名称）
                    env.buName = "${JOB_NAME}".split('-')[0]
                    // 服务/项目名称
                    env.serviceName = "${JOB_NAME}".split('_')[0]

                    // Git项目Id
                    env.projectId = projectCustom.getProjectIdByProjectName("${env.serviceName}")
                    if ("${env.projectId}" == "null") {
                        env.projectId = gitlab.GetProjectId("${env.gitlabUserTokenCredentialsId}", "${env.buName}", "${env.serviceName}")
                    }
                    println("projectId：${env.projectId}")

                    // Git提交ID
                    env.commitId = gitlab.GetShortCommitIdByApi("${env.gitlabUserTokenCredentialsId}", "${env.projectId}", "${params.branchName}")
                    // 服务版本号（推荐定义："${branchName}-${commitId}"）
                    env.version = "${params.branchName}-${env.commitId}"

                    // 服务访问的域名
                    accessDomainName = projectCustom.getAccessDomainName("${params.domainName}")
                    if ("${accessDomainName}" == "null") {
                        env.accessDomainName = "${params.domainName}.${params.envList}"
                    } else {
                        env.accessDomainName = "${accessDomainName}.${params.envList}"
                    }
                    env.domainName = "${params.domainName}.${params.envList}"

                    // 服务部署的命名空间
                    env.namespace = "${env.buName}-${params.envList}"

                    // 修改Jenkins构建描述
                    currentBuild.description = """ branchName：${params.branchName} \n commitId：${env.commitId} \n namespace：${env.namespace} \n domainName：${env.domainName} """
                    // 修改Jenkins构建名称
                    currentBuild.displayName = "${env.version}"
                }
            }
        }

        stage("K8sReleaseFile") {
            steps {
                script {
                    // Git项目Id（devops-k8s-deployment）
                    k8sProjectId = gitlab.GetProjectId("${env.gitlabUserTokenCredentialsId}", "devops", "devops-k8s-deployment")
                    // 下载Kubernetes部署文件
                    env.deployFileName = "${env.version}.yaml"
                    // 文件路径：项目服务名称/版本号.yaml
                    filePath = "${env.serviceName}%2fNative%2f${env.version}.yaml"
                    // 下载Kubernetes部署模板文件
                    fileData = gitlab.GetRepositoryFile("${env.gitlabUserTokenCredentialsId}", "${k8sProjectId}", "${filePath}", "main")
                    kubernetes.K8sReleaseTemplateFileReplace("${env.deployFileName}", "${fileData}", "${env.domainName}",
                            "${params.port}", "${env.serviceName}", "${env.namespace}")
                }
            }
        }

        stage("K8sDeploy") {
            steps {
                script {
                    kubernetes.KubernetesDeploy("${env.buName}", "${env.deployFileName}", "${env.serviceName}")
                }
            }
        }

        stage("HealthCheck") {
            steps {
                script {
                    // 注意：自定义域名需配置Hosts文件！
                    try {
                        sleep(5)
                        result = sh returnStdout: true, script: """ curl "http://${env.domainName}/health" """ - "\n"
                        if ("ok" == result) {
                            println("Successful！")
                        }
                    } catch (Exception e) {
                        println(e)
                    }
                }
            }
        }

        stage("RollOut") {
            input {
                message "是否进行回滚"
                ok "提交"
                submitter "admin,myst"
                parameters {
                    choice(choices: ['no', 'yes'], name: 'opts')
                }
            }

            steps {
                script {
                    switch ("${opts}") {
                        case "yes":
                            sh "kubectl rollout undo deployment/${env.serviceName} -n ${env.buName}"
                            break
                        case "no":
                            break
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