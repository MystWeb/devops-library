package org.devops

/**
 * 获取CommitId
 */
def GetCommitId() {
    commitId = sh returnStdout: true, script: "git rev-parse HEAD"
    return commitId - "\n"
}

/**
 * 获取Commit简短id
 * 命令：git rev-parse --short HEAD，输出：7位数commitId
 */
def GetShortCommitId() {
    commitId = sh returnStdout: true, script: "git rev-parse --short HEAD"
    return commitId - "\n"
}

/**
 * 获取Commit简短id
 */
def GetShortCommitIdByEightDigit() {
    commitId = sh returnStdout: true, script: "git rev-parse HEAD"
    commitId = commitId - "\n"
    return commitId[0..7]
}

/**
 * 通过Api获取Commit简短id
 * @param projectId 项目id
 * @param branchName 分支名称
 */
def GetShortCommitIdByApi(projectId, branchName) {
    apiUrl = "projects/${projectId}/repository/branches/${branchName}"
    response = GitLabRequest("GET", apiUrl)
    return response.commit.short_id - "\n"
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
    withCredentials([string(credentialsId: '926a978a-5cef-49ca-8ff8-5351ed0700bf', variable: 'GITLAB_SONAR_TOKEN')]) {
        apiUrl = "projects?search=${projectName}"
        response = GitLabRequest("GET", apiUrl)
        if (response != []) {
            for (r in response) {
                if (r["namespace"]["name"] == groupName) {
                    return response[0]["id"]
                }
            }
        }
    }

}

/**
 * GitLabRestApi GitLab请求
 * @param method 请求方法
 * @param apiUrl API URL
 */
def GitLabRequest(method, apiUrl) {
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