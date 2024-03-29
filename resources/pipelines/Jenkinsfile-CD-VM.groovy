// 加载共享库
@Library("mylib@main") _

// 导入库
import org.devops.Ansible
import org.devops.Artifact
import org.devops.GitLab
import org.devops.SaltStack

// New实例化
def gitlab = new GitLab()
def artifact = new Artifact()
def ansible = new Ansible()
def saltStack = new SaltStack()

pipeline {
    agent {
        label "build"
    }

    options {
        skipDefaultCheckout true
        buildDiscarder logRotator(daysToKeepStr: '30', numToKeepStr: '30')
    }

    parameters {
        string defaultValue: 'RELEASE-1.1.1', description: '注意：选择发布分支', name: 'branchName'
        choice choices: ['jar', 'war', 'html', 'go', 'py'], description: '注意：选择制品类型', name: 'artifactType'
        /*choice choices: ['uat', 'stag', 'prod'], description: '注意：选择发布环境', name: 'envList'
        extendedChoice defaultValue: '192.168.20.158,192.168.20.191',
                description: '注意：选择远程发布主机', multiSelectDelimiter: ',',
                name: 'targetHosts', quoteValue: false, saveJSONParameterToFile: false,
                type: 'PT_CHECKBOX', value: '192.168.20.158,192.168.20.191,node01,node02', visibleItemCount: 10*/
        /*extendedChoice defaultValue: 'node01,node02',
                description: '注意：选择远程发布主机', multiSelectDelimiter: ',',
                name: 'targetHosts', quoteValue: false, saveJSONParameterToFile: false,
                type: 'PT_CHECKBOX', value: 'node01,node02', visibleItemCount: 10*/
        choice choices: ['/opt', '/tmp'], description: '注意：选择远程主机的发布目录', name: 'targetDir'
        string defaultValue: '8090', description: '注意：服务监听的端口号', name: 'port'
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
                    // GitLab用户Token访问凭据Id
                    env.gitlabUserTokenCredentialsId = "926a978a-5cef-49ca-8ff8-5351ed0700bf"
                    // Git项目Id
                    env.projectId = gitlab.GetProjectId("${env.gitlabUserTokenCredentialsId}", "${env.buName}", "${env.serviceName}")
                    // Git提交ID
                    env.commitId = gitlab.GetShortCommitIdByApi("${env.gitlabUserTokenCredentialsId}", "${env.projectId}", "${env.branchName}")
                    // 服务版本号（推荐定义："${branchName}-${commitId}"）
                    env.version = "${env.branchName}-${env.commitId}"

                    // 文件路径
                    env.filePath = "${env.buName}/${env.serviceName}/${env.version}"
                    // 文件名称
                    env.fileName = "${env.serviceName}-${env.version}.${env.artifactType}"

                    // 制品仓库地址
                    env.artifactRegistry = "192.168.20.194:8081"
                    // 制品仓库访问凭据Id
                    env.artifactCredentialsId = "0cbf60e3-319d-464a-8efe-cf83ebeb97ff"
                    // 制品仓库名称
                    env.artifactRepository = "devops-local"
                    // 镜像仓库地址
                    env.imageRegistry = "192.168.20.194:8088"

                    // 修改Jenkins构建描述
                    currentBuild.description = """ branchName：${env.branchName} \n commitId：${env.commitId} """
                    // 修改Jenkins构建名称
                    currentBuild.displayName = "${env.version}"
                }
            }
        }

        stage("PullArtifact") {
            steps {
                script {
                    artifact.PullArtifactByApi("${env.artifactRegistry}", "${env.artifactCredentialsId}", "${env.artifactRepository}",
                            "${env.filePath}", "${env.fileName}")
                }
            }
        }

        stage("Deploy") {
            steps {
                script {
                    if (null == "${env.targetHosts}" || "${env.targetHosts}".trim().length() <= 0) {
                        println("The deployment host is not selected.")
                    } else {
                        ansible.AnsibleDeploy("${env.targetHosts}", "${env.targetDir}",
                                "${env.serviceName}", "${env.version}",
                                "${env.fileName}", "${env.port}")

                        // SaltStack 远程多主机部署（扩展）
                        /*saltStack.SaltStackDeploy("${env.targetHosts}", "${env.targetDir}",
                                "${env.serviceName}", "${env.version}",
                                "${env.fileName}", "${env.port}")*/
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
    }

}