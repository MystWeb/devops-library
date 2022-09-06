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
 * 获取ProjectId
 * git fork：user-a/devops-service-app -> user-b/devops-service-app
 *
 * @param groupName 组名称/命名空间
 * @param projectName 项目名称
 * @param token GitLab-Sonar-Token
 */
def GetProjectId(groupName, projectName) {
    withCredentials([string(credentialsId: '926a978a-5cef-49ca-8ff8-5351ed0700bf', variable: 'GITLAB_SONAR_TOKEN')]) {
        response = sh returnStdout: true,
                script: """
                curl --location --request GET \
                http://192.168.20.194/api/v4/projects?search=${projectName} \
                --header "PRIVATE-TOKEN: ${GITLAB_SONAR_TOKEN}"
            """
        response = readJSON text: response - "\n"
        if (response != []) {
            for (r in response) {
                if (r["namespace"]["name"] == groupName) {
                    return response[0]["id"]
                }
            }
        }
    }

}