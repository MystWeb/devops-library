pipeline {
    agent {
        label "build"
    }

    stages {
        stage("PullArtifact") {
            steps {
                script {
                    // JOB任务前缀（业务名称/组名称）
                    env.buName = "${JOB_NAME}".split('-')[0]
                    env.serviceName = "${JOB_NAME}".split('_')[0]
                    env.projectId = GetProjectId("${env.buName}", "${env.serviceName}")
                    env.commitId = GetShortCommitIdByApi("${env.projectId}", "${env.branchName}")
                    env.path = "${env.buName}/${env.serviceName}/${env.branchName}-${env.commitId}"
                    // TODO .jar 后缀名动态处理
                    env.packageName = "${env.serviceName}-${env.branchName}-${env.commitId}.jar"
                    PullArtifact("${env.path}", "${env.packageName}")
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

/**
 * 获取ProjectId
 * git fork：user-a/devops-service-app -> user-b/devops-service-app
 *
 * @param groupName 组名称/命名空间
 * @param projectName 项目名称
 * @param token GitLab-Sonar-Token
 */
def GetProjectId(groupName, projectName) {
    apiUrl = "projects?search=${projectName}"
    response = HttpReq("GET", apiUrl)
    if (response != []) {
        for (r in response) {
            if (r["namespace"]["name"] == groupName) {
                return response[0]["id"]
            }
        }
    }
}

/**
 * 通过Api获取Commit简短id
 * @param projectId 项目id
 * @param branchName 分支名称
 */
def GetShortCommitIdByApi(projectId, branchName) {
    apiUrl = "projects/${projectId}/repository/branches/${branchName}"
    response = HttpReq("GET", apiUrl)
    shortId = response.commit.short_id - "\n"
    // 命令：git rev-parse --short HEAD，输出：7位数commitId
    return shortId[0..6]
}

def HttpReq(method, apiUrl) {
    withCredentials([string(credentialsId: '926a978a-5cef-49ca-8ff8-5351ed0700bf', variable: 'GITLAB_SONAR_TOKEN')]) {
        response = sh returnStdout: true,
                script: """
                curl --location --request ${method} \
                http://192.168.20.194/api/v4/${apiUrl} \
                --header "PRIVATE-TOKEN: ${GITLAB_SONAR_TOKEN}"
            """
        response = readJSON text: response - "\n"
        return response
    }
}

def PullArtifact(path, packageName) {
    println("开始下载：http://192.168.20.194:8081/repository/devops-local/${path}/${packageName}")
    sh """
        curl http://192.168.20.194:8081/repository/devops-local/${path}/${packageName} \
        -u admin:proaim@2013 \
        -o ${packageName} -s
    """
    println("下载完成：http://192.168.20.194:8081/repository/devops-local/${path}/${packageName}")
}