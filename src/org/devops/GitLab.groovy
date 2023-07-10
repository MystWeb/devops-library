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
 * 通过Api获取CommitId
 * @param credentialsId GitLab用户Token访问凭据Id
 * @param projectId 项目id
 * @param branchName 分支名称
 */
def GetCommitIdByApi(credentialsId, projectId, branchName) {
    apiUrl = "projects/${projectId}/repository/branches/${branchName}"
    response = GitLabRequest("${credentialsId}", "GET", "${apiUrl}")
    response = readJSON text: response - "\n"
    return response.commit.id - "\n"
}

/**
 * 通过Api获取Commit简短id
 * @param credentialsId GitLab用户Token访问凭据Id
 * @param projectId 项目id
 * @param branchName 分支名称
 */
def GetShortCommitIdByApi(credentialsId, projectId, branchName) {
    apiUrl = "projects/${projectId}/repository/branches/${branchName}"
    response = GitLabRequest("${credentialsId}", "GET", "${apiUrl}")
    response = readJSON text: response - "\n"
    return response.commit.short_id - "\n"
}

/**
 * 通过Api获取Commit Web URL（commit提交超链接）
 * @param credentialsId GitLab用户Token访问凭据Id
 * @param projectId 项目id
 * @param branchName 分支名称
 */
def GetCommitWebURLByApi(credentialsId, projectId, branchName) {
    apiUrl = "projects/${projectId}/repository/branches/${branchName}"
    response = GitLabRequest("${credentialsId}", "GET", "${apiUrl}")
    response = readJSON text: response - "\n"
    return response.commit.web_url - "\n"
}

/**
 * 获取ProjectId
 * git fork：user-a/devops-service-app -> user-b/devops-service-app
 *
 * @param credentialsId GitLab用户Token访问凭据Id
 * @param groupName 组名称/命名空间
 * @param projectName 项目名称
 * @param token GitLab-Sonar-Token
 */
def GetProjectId(credentialsId, groupName, projectName) {
    withCredentials([string(credentialsId: "${credentialsId}", variable: 'GITLAB_USER_TOKEN')]) {
        apiUrl = "projects?search=${projectName}"
        response = GitLabRequest("${credentialsId}", "GET", "${apiUrl}")
        response = readJSON text: response - "\n"
        if (response != []) {
            for (r in response) {
                if (r["namespace"]["name"] == "${groupName}") {
                    return response[0]["id"]
                }
            }
        }
    }

}

/**
 * GitLabRestApi GitLab请求
 * @param credentialsId GitLab用户Token访问凭据Id
 * @param method 请求方法
 * @param apiUrl API URL
 */
def GitLabRequest(credentialsId, method, apiUrl) {
    withCredentials([string(credentialsId: "${credentialsId}", variable: 'GITLAB_USER_TOKEN')]) {
        // GitLab仓库地址
        registry = "http://192.168.100.150"

        response = sh returnStdout: true,
                script: """
                curl --location --request ${method} \
                ${registry}/api/v4/${apiUrl} \
                --header "PRIVATE-TOKEN: ${GITLAB_USER_TOKEN}"
            """
        println("GitLabRequest：${response}")
        return response
    }
}

/**
 * 获取存储库文件
 * GET /projects/:id/repository/files/:file_path/raw
 * @param credentialsId GitLab用户Token访问凭据Id
 * @param projectId 项目Id
 * @param filePath 文件路径
 * @param branchName 分支名称
 */
def GetRepositoryFile(credentialsId, projectId, filePath, branchName) {
    apiUrl = "/projects/${projectId}/repository/files/${filePath}/raw?ref=${branchName}"
    response = GitLabRequest("${credentialsId}", 'GET', "${apiUrl}")
    return response
}

/**
 * GitLabApi HTTP/HTTPS 请求
 * 插件链接：https://plugins.jenkins.io/http_request
 * @param credentialsId GitLab用户Token访问凭据Id
 * @param method 请求方法
 * @param apiUrl 请求地址
 * @param requestBody 请求体
 */
def GitLabHttpRequest(credentialsId, method, apiUrl, requestBody) {
    // GitLab接口地址
    def gitServer = "http://192.168.100.150/api/v4"
    withCredentials([string(credentialsId: "${credentialsId}", variable: 'GITLAB_USER_TOKEN')]) {
        response = httpRequest acceptType: 'APPLICATION_JSON_UTF8',
                consoleLogResponseBody: true,
                contentType: 'APPLICATION_JSON_UTF8',
                customHeaders: [[maskValue: false, name: 'PRIVATE-TOKEN', value: "${GITLAB_USER_TOKEN}"]],
                httpMode: "${method}",
                url: "${gitServer}/${apiUrl}",
                wrapAsMultipart: false,
                requestBody: "${requestBody}"
    }
    println("GitLabHttpRequest：${response}")
    return response
}

/**
 * 创建存储库文件
 * @param credentialsId GitLab用户Token访问凭据Id
 * @param projectId 项目Id
 * @param branchName 分支名称
 * @param filePath 文件路径
 * @param fileContent 文件内容
 */
def CreateRepositoryFile(credentialsId, projectId, branchName, filePath, fileContent) {
    apiUrl = "projects/${projectId}/repository/files/${filePath}"
    requestBody = """{"branch": "${branchName}", "encoding":"base64", "content": "${fileContent}", "commit_message": "update a new file"}"""
    response = GitLabHttpRequest("${credentialsId}", 'POST', "${apiUrl}", "${requestBody}")
    println(response)
}

/**
 * 更新存储库文件
 * @param credentialsId GitLab用户Token访问凭据Id
 * @param projectId 项目Id
 * @param branchName 分支名称
 * @param filePath 文件路径
 * @param fileContent 文件内容
 */
def UpdateRepositoryFile(credentialsId, projectId, branchName, filePath, fileContent) {
    apiUrl = "projects/${projectId}/repository/files/${filePath}"
    requestBody = """{"branch": "${branchName}", "encoding":"base64", "content": "${fileContent}", "commit_message": "update a new file"}"""
    response = GitLabHttpRequest("${credentialsId}", 'PUT', "${apiUrl}", "${requestBody}")
    println(response)
}