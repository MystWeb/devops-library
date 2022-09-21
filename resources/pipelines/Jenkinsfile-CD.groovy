// 加载共享库
@Library("mylib@main") _

// 导入库

import org.devops.Artifact
import org.devops.GitLab

// New实例化
def gitlab = new GitLab()
def artifact = new Artifact()

pipeline {
    agent {
        label "build"
    }

    options {
        skipDefaultCheckout true
    }

    parameters {
        string defaultValue: 'main', description: '注意：选择发布分支', name: 'branchName'
        choice choices: ['jar', 'war', 'html', 'go', 'py'], description: '注意：选择制品类型', name: 'artifactType'
        choice choices: ['uat', 'stag', 'prod'], description: '注意：选择发布环境', name: 'envList'
        extendedChoice defaultValue: '192.168.20.158,192.168.20.191',
                description: '注意：选择发布主机', multiSelectDelimiter: ',',
                name: 'deployHosts', quoteValue: false, saveJSONParameterToFile: false,
                type: 'PT_CHECKBOX', value: '192.168.20.158,192.168.20.191', visibleItemCount: 10
        choice choices: ['/opt', '/tmp'], description: '注意：选择远程主机的发布目录', name: 'targetDir'
    }

    stages {
        stage("Global") {
            steps {
                script {
                    // 任务名称截取构建类型（任务名称示例：devops-maven-service）
//                    env.buildType = "${JOB_NAME}".split("-")[1]
                    // JOB任务前缀（业务名称/组名称）
                    env.buName = "${JOB_NAME}".split('-')[0]
                    env.serviceName = "${JOB_NAME}".split('_')[0]
                    env.projectId = gitlab.GetProjectId("${env.buName}", "${env.serviceName}")
                    env.commitId = gitlab.GetShortCommitIdByApi("${env.projectId}", "${env.branchName}")
                    // 修改Jenkins构建描述
                    currentBuild.description = """branchName：${env.branchName} \n"""
                    // 修改Jenkins构建名称
                    currentBuild.displayName = "${env.commitId}"
                    // 文件路径
                    env.filePath = "${env.buName}/${env.serviceName}/${env.branchName}-${env.commitId}"
                    // 文件名称
                    env.fileName = "${env.serviceName}-${env.branchName}-${env.commitId}.${env.artifactType}"
                }
            }
        }

        stage("PullArtifact") {
            steps {
                script {
                    artifact.PullArtifactByApi("${filePath}", "${fileName}")
                }
            }
        }

        stage("AnsibleDeploy") {
            steps {
                script {
                    println("deploy ${env.envList}")
                    if (null == "${env.deployHosts}" || "${env.deployHosts}".trim().length() <= 0) {
                        println("The deployment host is not selected.")
                    } else {
                        // 删除旧的hosts
                        sh "rm -fr hosts"
                        // 根据选择的发布主机生成hosts
                        for (final def host in "${env.deployHosts}".split(',')) {
                            sh "echo ${host} >> hosts"
                        }
                        sh "cat hosts"

                        // ansible 发布
                        sh """
                            # 主机连通性检测
                            ansible "${env.deployHosts}" -m ping -i hosts
                            
                            # 清理和创建发布目录
                            ansible "${env.deployHosts}" -m shell -a "rm -fr ${env.targetDir}/${env.serviceName}/* \\ 
                                && mkdir -p ${env.targetDir}/${env.serviceName} || echo file is exists"
                            
                            # 复制应用文件
                            ansible "${env.deployHosts}" -m copy -a "src=${env.fileName} dest=${env.targetDir}/${env.serviceName}/${fileName}"
                        """
                    }
                }
            }
        }
    }
}