package org.devops


def checkVueTscLint() {
    echo "Checking Vue and TypeScript lint..."

    // 执行 lint:vue-tsc 命令
    sh 'npm run lint:vue-tsc'
}

/**
 * SonarQube指标&通知
 * @param sonarHostUrl SonarQube访问地址
 * @param projectKey SonarQube项目key
 * @param branchName 分支名称
 * @param mergeRequestId GitLab Merge Request ID（适用于 PR 分支）
 * @param sonarqubeUserTokenCredentialsId SonarQube用户Token凭据ID
 * @param dingTalkRobotIdCredentialsId 钉钉机器人Token凭据ID
 */
def SonarQubeMetricsAndNotify(sonarHostUrl, projectKey, branchName, mergeRequestId, sonarqubeUserTokenCredentialsId, dingTalkRobotIdCredentialsId) {
    // 安全转换函数
    def safeToInt = { str ->
        try {
            return str?.isInteger() ? str.toInteger() : 0
        } catch (e) {
            return 0
        }
    }

    def safeToDouble = { str ->
        try {
            return str?.isDouble() ? str.toDouble() : 0.0
        } catch (e) {
            return 0.0
        }
    }

    withCredentials([string(credentialsId: "${sonarqubeUserTokenCredentialsId}", variable: 'SONARQUBE_USER_TOKEN')]) {
        // 获取 SonarQube 扫描结果
        def json = null
        if (branchName != null && mergeRequestId == null) {
            json = sh(script: """
            curl -s -u ${SONARQUBE_USER_TOKEN}: "${sonarHostUrl}/api/measures/component?component=${projectKey}&branch=${branchName}&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density"
        """, returnStdout: true).trim()
            currentBuild.description = "[🔍 SonarQube 分析报告](${sonarHostUrl}/dashboard?branch=${branchName}&id=${projectKey})"
        } else if (mergeRequestId != null) {
            json = sh(script: """
            curl -s -u ${SONARQUBE_USER_TOKEN}: "${sonarHostUrl}/api/measures/component?component=${projectKey}&pullRequest=${mergeRequestId}&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density"
        """, returnStdout: true).trim()
            currentBuild.description = "[🔍 SonarQube 分析报告](${sonarHostUrl}/dashboard?id=${projectKey}&pullRequest=${mergeRequestId})"
        } else {
            error "❌ 分支名称和合并请求ID都未提供，无法获取 SonarQube 扫描结果！请检查参数设置。"
        }
        echo "获取扫描结果: ${json}"

        // 解析 JSON 数据
        def jsonObject = readJSON(text: json)
        def metrics = ['bugs', 'vulnerabilities', 'code_smells', 'coverage', 'duplicated_lines_density'].collectEntries {
            [(it): jsonObject.component.measures.find { measure -> measure.metric == it }?.value ?: '0']
        }

        // 颜色逻辑
        def bugColor = safeToInt(metrics.bugs) > 0 ? 'red' : 'green'
        def vulnerabilityColor = safeToInt(metrics.vulnerabilities) > 0 ? 'red' : 'green'
        def codeSmellColor = safeToInt(metrics.code_smells) > 0 ? 'orange' : 'green'
        def coverageColor = safeToDouble(metrics.coverage) < 80.0 ? 'orange' : 'green'
        def duplicatedLinesColor = safeToDouble(metrics.duplicated_lines_density) > 10.0 ? 'red' : 'green'

        // 构建通知消息
        def buildColor = "${currentBuild.currentResult}" == 'SUCCESS' ? 'green' : "${currentBuild.currentResult}" == 'FAILURE' ? 'red' : 'orange'
        def message = [
                "### SonarQube 扫描结果 - ${projectKey}",
                "- **分支**: ${branchName ?: 'N/A'}",
                "- **Bugs**: <font color=${bugColor}>${metrics.bugs}</font>",
                "- **Vulnerabilities**: <font color=${vulnerabilityColor}>${metrics.vulnerabilities}</font>",
                "- **Code Smells**: <font color=${codeSmellColor}>${metrics.code_smells}</font>",
                "- **Coverage**: <font color=${coverageColor}>${metrics.coverage}%</font>",
                "- **Duplicated Lines Density**: <font color=${duplicatedLinesColor}>${metrics.duplicated_lines_density}%</font>",
                "---",
                "- 任务：[${currentBuild.displayName}](${env.BUILD_URL})",
                "- 状态：<font color=${buildColor}>${currentBuild.currentResult}</font>",
                "- 持续时间：${currentBuild.durationString.split('and counting')[0]}",
                "- 执行人：${currentBuild.buildCauses.shortDescription}",
                "- 描述：${currentBuild.description ?: '无描述'}",
        ]

        new Notice().dingTalkPluginNotice("${dingTalkRobotIdCredentialsId}", "SonarQube 扫描结果 - ${projectKey}", message)
    }
}

/**
 * 代码扫描-Sonar（使用 Maven）
 * @param sonarqubeUserTokenCredentialsId SonarQube访问凭据Id
 * @param gitlabUserTokenCredentialsId GitLab用户Token访问凭据Id
 * @param projectVersion 代码扫描-Sonar-项目版本（推荐使用分支名称）
 * @param commitId 提交Id
 * @param projectId 项目Id
 * @param mavenPath Maven路径（可选，默认为"/opt/apache-maven-3.8.8/bin/mvn"）
 * @param jdkHome JDK路径（可选，默认为"/opt/jdk-11.0.19"）
 * 插件链接：https://github.com/mc1arke/sonarqube-community-branch-plugin、
 * https://github.com/xuhuisheng/sonar-l10n-zh、
 * https://github.com/gabrie-allaigre/sonar-gitlab-plugin
 */
def CodeScan_Sonar_Maven(sonarqubeUserTokenCredentialsId, gitlabUserTokenCredentialsId, projectVersion, commitId, projectId, mavenPath = "/opt/apache-maven-3.8.8/bin/mvn", jdkHome = "/opt/jdk-11.0.19") {
    withCredentials([string(credentialsId: "${sonarqubeUserTokenCredentialsId}", variable: 'SONARQUBE_USER_TOKEN'),
                     string(credentialsId: "${gitlabUserTokenCredentialsId}", variable: 'GITLAB_USER_TOKEN')]) {
        // 使用 Maven 执行 SonarQube 扫描
        sh """
            ${mavenPath} clean -Dpmd.skip=true -Dcheckstyle.skip=true -DskipTests -Djaxb2.skip=true \
            verify sonar:sonar \
            -Dsonar.login=${SONARQUBE_USER_TOKEN} \
            -Dsonar.projectVersion=${projectVersion} \
            -Dsonar.branch.name=${projectVersion} \
            -Dsonar.gitlab.commit_sha=${commitId} \
            -Dsonar.gitlab.ref_name=${projectVersion} \
            -Dsonar.gitlab.project_id=${projectId} \
            -Dsonar.gitlab.user_token=${GITLAB_USER_TOKEN} \
            -Dsonar.java.jdkHome=${jdkHome}
        """
    }
}

/**
 * 代码扫描-Sonar
 * @param sonarqubeUserTokenCredentialsId SonarQube访问凭据Id
 * @param gitlabUserTokenCredentialsId GitLab用户Token访问凭据Id
 * @param projectVersion 代码扫描-Sonar-项目版本（推荐使用分支名称）
 * @param commitId 提交Id
 * @param projectId 项目Id
 * 插件链接：https://github.com/mc1arke/sonarqube-community-branch-plugin、
 * https://github.com/xuhuisheng/sonar-l10n-zh、
 * https://github.com/gabrie-allaigre/sonar-gitlab-plugin
 * https://plugins.jenkins.io/sonar/
 */
def CodeScan_Sonar(sonarqubeUserTokenCredentialsId, gitlabUserTokenCredentialsId, projectVersion, commitId, projectId) {

    // 如果分支名为空，直接终止构建
    if (projectVersion == null || projectVersion.trim() == "") {
        error "❌ 检测到分支名为空，SonarQube 扫描已终止！请检查 GitLab webhook 参数注入是否正确。"
    }

    cliPath = "/opt/sonar-scanner/bin"
    withSonarQubeEnv('SonarQube') { // Jenkins系统配置-SonarQube servers已配置的Name
        withCredentials([string(credentialsId: "${sonarqubeUserTokenCredentialsId}", variable: 'SONARQUBE_USER_TOKEN'),
                         string(credentialsId: "${gitlabUserTokenCredentialsId}", variable: 'GITLAB_USER_TOKEN')]) {
            // 远程构建时推荐使用CommitID作为代码扫描-项目版本
            try {
                sh """
                    ${cliPath}/sonar-scanner \
                    -Dsonar.login=${SONARQUBE_USER_TOKEN} \
                    -Dsonar.host.url=${env.SONAR_HOST_URL} \
                    -Dsonar.projectVersion=${projectVersion} \
                    -Dsonar.branch.name=${projectVersion} \
                    -Dsonar.gitlab.commit_sha=${commitId} \
                    -Dsonar.gitlab.ref_name=${projectVersion} \
                    -Dsonar.gitlab.project_id=${projectId} \
                    -Dsonar.gitlab.user_token=${GITLAB_USER_TOKEN}
                """
            } catch (e) {
                error "SonarQube 代码扫描失败: ${e.getMessage()}"
            }
        }
    }
}

/**
 * 获取合并请求信息，用于判断是否需要执行代码扫描
 * @param actionType 触发类型（PUSH/MERGE）
 * @param sourceBranch 源分支名称
 * @param targetBranch 目标分支名称
 * @param mergeRequestId 合并请求ID
 * @param projectId GitLab项目ID
 * @param srcUrl 源代码仓库URL
 * @param gitlabUserTokenCredentialsId GitLab用户令牌凭证ID
 * @return 包含合并请求信息的Map
 */
def getMergeRequestInfo(actionType, sourceBranch, targetBranch,
                        mergeRequestId, projectId, srcUrl,
                        gitlabUserTokenCredentialsId) {
    def isPush = actionType == "PUSH"
    def isMerge = actionType == "MERGE"
    targetBranch = targetBranch ?: "main"

    if (isMerge && mergeRequestId) {
        return [sourceBranch: sourceBranch, targetBranch: targetBranch, mergeRequestId: mergeRequestId, performScan: true]
    }

    if (isPush) {
        def performScan = false
        def mrId = ""
        withCredentials([string(credentialsId: gitlabUserTokenCredentialsId, variable: 'GITLAB_TOKEN')]) {
            def host = srcUrl.replaceFirst(/^https?:\/\//, '').replaceFirst(/^git@/, '').tokenize(/[:\/]/)[0]
            def api = "https://${host}/api/v4/projects/${projectId}/merge_requests?state=opened"

            def openMRs = sh(script: """curl -s --header "PRIVATE-TOKEN: \$GITLAB_TOKEN" "${api}" | jq -c '.[]'""", returnStdout: true).trim()
            if (openMRs) {
                openMRs.readLines().each {
                    def mr = readJSON text: it
                    if (mr.source_branch == sourceBranch) {
                        performScan = true
                        targetBranch = mr.target_branch
                        mrId = mr.iid.toString()
                        echo "✅ 匹配到 MR #${mrId}：${sourceBranch} → ${targetBranch}"
                        return
                    }
                }
            }
        }
        return [sourceBranch: sourceBranch, targetBranch: targetBranch, mergeRequestId: mrId, performScan: performScan]
    }

    return [sourceBranch: sourceBranch, targetBranch: targetBranch, mergeRequestId: mergeRequestId, performScan: false]
}

/**
 * 跳过未更改的代码扫描-Sonar
 * @param sonarqubeUserTokenCredentialsId SonarQube访问凭据Id
 * @param gitlabUserTokenCredentialsId GitLab用户Token访问凭据Id
 * @param projectVersion 代码扫描-Sonar-项目版本（推荐使用分支名称）
 * @param commitId 提交Id
 * @param projectId 项目Id
 * 插件链接：https://github.com/mc1arke/sonarqube-community-branch-plugin、
 * https://github.com/xuhuisheng/sonar-l10n-zh、
 * https://github.com/gabrie-allaigre/sonar-gitlab-plugin
 */
def scanCodeWithSonarSkipUnchanged(sonarqubeUserTokenCredentialsId, gitlabUserTokenCredentialsId, commitId, projectId, sourceBranch, targetBranch, gitlabMergeRequestId) {
    cliPath = "/opt/sonar-scanner/bin"

    // 安全检查分支名
    if (!targetBranch || !sourceBranch) {
        error "目标分支和源分支不能为空"
    }

    // 获取 MR 变更文件列表（使用 git diff 对比源分支和目标分支）
    def changedFiles = sh(
            script: """    
                # 获取变更文件并过滤特定类型
                diff_output=\$(git diff --name-only origin/${targetBranch} origin/${sourceBranch})
                if [ -z "\$diff_output" ]; then
                    echo ""
                else
                    echo "\$diff_output" | grep -E '\\.(java|xml|properties|groovy)\$' || echo ""
                fi
            """,
            returnStdout: true).trim()
    if (changedFiles == "") {
        echo "⚠️ 无代码变更文件，跳过 Sonar 扫描"
        return
    }
    changedFiles = changedFiles.replace('\n', ',')
    echo "变更文件：${changedFiles}"
    // 将变更文件列表写入 inclusions.txt 文件，避免 SonarQube 扫描时参数列表过长
//    writeFile file: 'inclusions.txt', text: changedFiles.readLines().join(',\n')
//    def inclusionStr = readFile('inclusions.txt').trim()
//    echo "变更文件合并：${inclusionStr}"

    withSonarQubeEnv('SonarQube') { // 让 Jenkins 自动提供 SonarQube 地址
        withCredentials([string(credentialsId: "${sonarqubeUserTokenCredentialsId}", variable: 'SONARQUBE_USER_TOKEN'),
                         string(credentialsId: "${gitlabUserTokenCredentialsId}", variable: 'GITLAB_USER_TOKEN')]) {
            try {
                sh """
                    ${cliPath}/sonar-scanner \
                    -Dsonar.login=${SONARQUBE_USER_TOKEN} \
                    -Dsonar.host.url=${env.SONAR_HOST_URL} \
                    -Dsonar.projectVersion=${sourceBranch} \
                    -Dsonar.pullrequest.provider=GitLab \
                    -Dsonar.pullrequest.key=${gitlabMergeRequestId} \
                    -Dsonar.pullrequest.branch=${sourceBranch} \
                    -Dsonar.pullrequest.base=${targetBranch} \
                    -Dsonar.inclusions="${changedFiles}" \
                    -Dsonar.gitlab.commit_sha=${commitId} \
                    -Dsonar.gitlab.project_id=${projectId} \
                    -Dsonar.gitlab.user_token=${GITLAB_USER_TOKEN}
                """
            } catch (e) {
                error "SonarQube 代码扫描失败: ${e.getMessage()}"
            }
        }
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
    response = SonarRequest("POST", apiUrl)

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
    response = SonarRequest("POST", apiUrl)
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
    response = SonarRequest("GET", apiUrl)
    println("apiUrl：" + apiUrl + "\nresponse：" + response)
    if (response.paging.total == 0) {
        println("Project not found!.....")
        return false
    }
    return true
}
/**
 * SonarRestApi Sonar请求
 * @param method 请求方法
 * @param apiUrl API URL
 */
def SonarRequest(method, apiUrl) {
    // 通过ApiPost、PostMan等工具的Basic auth认证方式，输入Sonar用户名&密码后，
    // 生成代码-cURL的 --header 'Authorization: Basic *******=' 添加至Jenkins 凭据
    withCredentials([string(credentialsId: "f7acd2a7-576e-4908-9e80-ceab6525cc50", variable: 'SONAR_TOKEN')]) {
        // Sonar接口地址
        sonarApi = "http://192.168.100.150:9000/sonarqube/api"

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