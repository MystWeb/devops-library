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

    parameters {
        string defaultValue: 'main', description: '注意：选择发布分支', name: 'branchName'
        choice choices: ['jar', 'war', 'html', 'go', 'py'], description: '注意：选择制品类型', name: 'artifactType'
        choice choices: ['uat', 'stag', 'prod'], description: '注意：选择发布环境', name: 'envList'
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
                }
            }
        }

        stage("PullArtifact") {
            steps {
                script {
                    filePath = "${env.buName}/${env.serviceName}/${env.branchName}-${env.commitId}"
                    fileName = "${env.serviceName}-${env.branchName}-${env.commitId}.${env.artifactType}"
                    artifact.PullArtifactByApi("${filePath}", "${fileName}")
                }
            }
        }

        stage("Deploy") {
            steps {
                script {
                    println("deploy ${env.envList}")
                }
            }
        }
    }
}