package org.devops

/**
 * 代码扫描-Sonar
 * @param projectVersion 代码扫描-Sonar-项目版本（推荐使用分支名称）
 * 插件安装：https://github.com/mc1arke/sonarqube-community-branch-plugin、
 * https://github.com/xuhuisheng/sonar-l10n-zh、
 * https://github.com/gabrie-allaigre/sonar-gitlab-plugin
 */
def CodeScan_Sonar(projectVersion, commitId, projectId) {
    cliPath = "/data/cicd/sonar-scanner/bin"
    withCredentials([usernamePassword(credentialsId: '05d7379e-28a6-4dd2-9b35-1f907a1a05c8',
            usernameVariable: 'SONAR_USERNAME',
            passwordVariable: 'SONAR_PASSWORD'),
                     string(credentialsId: '926a978a-5cef-49ca-8ff8-5351ed0700bf',
                             variable: 'GITLAB_SONAR_TOKEN')]) {
        // 远程构建时推荐使用CommitID作为代码扫描-项目版本
        sh """
            ${cliPath}/sonar-scanner \
            -Dsonar.login=${SONAR_USERNAME} \
            -Dsonar.password=${SONAR_PASSWORD} \
            -Dsonar.projectVersion=${projectVersion} \
            -Dsonar.branch.name=${projectVersion} \
            -Dsonar.gitlab.commit_sha=${commitId} \
            -Dsonar.gitlab.ref_name=${projectVersion} \
            -Dsonar.gitlab.project_id=${projectId} \
            -Dsonar.gitlab.user_token=${GITLAB_SONAR_TOKEN}
        """
    }
}

/**
 * 初始化质量配置
 * @param lang 语言
 * @param projectName 项目名称
 * @param profileName 质量配置名称
 */
def InitQualityProfiles(lang, projectName, profileName) {
    result = ProjectSearch(projectName)
    println("InitQualityProfiles.ProjectSearch：" + result)

    if (result == false) {
        CreateProject(projectName)
    }

    UpdateQualityProfiles(lang, projectName, profileName)
}

/**
 * 更新项目质量配置
 * @param lang 语言
 * @param projectName 项目名称
 * @param profileName 质量配置名称
 */
def UpdateQualityProfiles(lang, projectName, profileName) {
    apiUrl = "qualityprofiles/add_project?language=${lang}&project=${projectName}&qualityProfile=${profileName}"
    response = SonarRequest(apiUrl, "POST")

    if (response.errors != true) {
        println("ERROR: UpdateQualityProfiles ${response.errors}...")
        return false
    } else {
        println("SUCCESS: UpdateQualityProfiles ${lang} > ${projectName} > ${profileName}")
        return true
    }
}

/**
 * 创建项目
 * @param projectName 项目名称
 */
def CreateProject(projectName) {
    apiUrl = "projects/create?name=${projectName}&project=${projectName}"
    response = SonarRequest(apiUrl, "POST")
    println("apiUrl：" + apiUrl + "\nresponse：" + response)
    try {
        if (response.project.key == projectName) {
            println("Project Create success!...")
            return true
        }
    } catch (e) {
        println(response.errors)
        return false
    }
}

/**
 * 查找项目
 * @param projectName 项目名称
 */
def ProjectSearch(projectName) {
    apiUrl = "projects/search?projects=${projectName}"
    response = SonarRequest(apiUrl, "GET")
    println("apiUrl：" + apiUrl + "\nresponse：" + response)
    if (response.paging.total == 0) {
        println("Project not found!.....")
        return false
    }
    return true
}
/**
 * SonarRestApi Sonar请求
 * @param apiUrl API URL
 * @param method 请求方法
 */
def SonarRequest(apiUrl, method) {
    // 通过ApiPost、PostMan等工具的Basic auth认证方式，输入Sonar用户名&密码后，
    // 生成代码-cURL的 --header 'Authorization: Basic *******=' 添加至Jenkins 凭据
    withCredentials([string(credentialsId: "d1ba0306-34e8-4030-a055-bd66d8d4c3a0", variable: 'SONAR_TOKEN')]) {
        sonarApi = "http://192.168.20.194:9000/api"
        response = sh returnStdout: true,
                script: """
                curl --location \
                    --request ${method} \
                    "${sonarApi}/${apiUrl}" \
                    --header "Authorization: Basic ${SONAR_TOKEN}"
                """
        try {
            // JSON数据格式化
            println("CodeScan.SonarRequest().try.response：" + response)
            if ("" != "${response}" || "${response}".trim().length() > 0) {
                response = readJSON text: """ ${response - "\n"} """
            } else {
                response = readJSON text: """{"errors" : true}"""
            }
        } catch (e) {
            response = readJSON text: """{"errors" : true}"""
            println("CodeScan.SonarRequest().catch：" + e)
        }
        return response
    }
}