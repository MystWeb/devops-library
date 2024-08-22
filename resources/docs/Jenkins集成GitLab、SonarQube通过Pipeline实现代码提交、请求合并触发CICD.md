# Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD与自研SonarQube指标&钉钉通知

## 效果展示

> 注意：这里只提供针对性核心内容！话不多说，直接上图！

### GitLab Trigger CI & 钉钉通知

![image-20240822173341124](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/image-20240822173341124.png)

![image-20240822172623389](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/image-20240822172623389.png)

### DevOps CI/CD & 钉钉通知

> 注意：源码已提供，但需要一定知识储备才可完整性实现 DevOps！

![image-20240822174723434](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/image-20240822174723434.png)

![image-20240822172946137](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/image-20240822172946137.png)

## Jenkins DevOps共享库源码

- https://github.com/MystWeb/devops-library
- https://github.com/MystWeb/k8s-ha-install
- https://github.com/MystWeb/kubernetes-deployments

## SonarQube

### SonarQube-用户令牌创建

![SonarQube-创建用户Token](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/SonarQube-创建用户Token.png)

### sonar-project.properties

- Maven Spring boot项目配置示例

```properties
# 定义唯一的关键字
sonar.projectKey=devops-web-backend

# 定义项目名称
sonar.projectName=devops-web-backend

# 定义项目的版本信息
# sonar.projectVersion=1.0

# 指定Sonar扫描时的JDK版本
sonar.java.jdkHome=/opt/jdk-11.0.19

# 指定扫描代码的目录位置（多个逗号分隔）
sonar.sources=.

# 执行项目编码
sonar.sourceEncoding=UTF-8

# 指定sonar Server
sonar.host.url=http://192.168.100.150:9000/sonarqube

# 认证信息
# sonar.login=SonarQube-Admin-Token

# 指定编译文件路径
sonar.exclusions=**/*.html,**/src/test/**
sonar.java.binaries=.
#sonar.java.binaries=target/classes
#sonar.java.test.binaries=target/test-classes
#sonar.java.surefire.report=target/surefire-reports

# 指定代码覆盖率工具
sonar.dynamicAnalysis=reuseReports
sonar.core.codeCoveragePlugin=jacoco
# 指定exec二进制文件存放路径（pom.xml设置的exec文件是在工程根目录下的target/coverage-reports/下）
#sonar.jacoco.reportPaths=target/coverage-reports/jacoco-unit.exec
# 指定代码仓库项目地址
sonar.links.homepage=http://192.168.100.150/devops/devops-web-backend
# 指定CI构建工具地址
sonar.links.ci=http://192.168.100.150:8080/job/devops-devops-web-backend_CICD/

# 指定GitLab Server
sonar.gitlab.url=http://192.168.100.150
# 指定失败通知模式
sonar.gitlab.failure_notification_mode=commit-status
# 指定GitLab Api版本
sonar.gitlab.api_version=v4
```

## GitLab

### GitLab-项目设置-Webhooks

- URL：Jenkins Pipeline JOB 开启 `Build when a change is pushed to GitLab. `设置中的 **GitLab Webhook-trigger URL**
- Secret令牌：Jenkins Pipeline JOB 开启 `Build when a change is pushed to GitLab. `设置中的
  -   `高级` | `Secret token` | `Generate` 按钮生成的Token值
- 按需勾选✔触发来源，推荐：推送事件、合并请求事件

![GitLab-项目设置-Webhooks](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/GitLab-项目设置-Webhooks.png)

### GitLab-项目设置-合并请求检查

> GitLab项目设置的合并检查，控制Jenkins Pipeline JOB远程触发构建后，Sonar Quality Gates 的结果决定是否允许合并分支

![GitLab-项目设置-合并请求检查](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/GitLab-项目设置-合并请求检查.png)

### GitLab-访问令牌-用户令牌创建

> 创建一个高权限的令牌，适用于各种通用场景，注意：令牌只会在生成时显示，注意本地保存收藏！

![GitLab-访问令牌-令牌创建](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/GitLab-访问令牌-令牌创建.png)

## Jenkins

### Jenkins-插件安装

- Jenkins 集成 GitLab：https://plugins.jenkins.io/gitlab-plugin

> 注意：Jenkins版本不兼容GitLab插件，可在官方下载

- SonarQube质量门结果：https://plugins.jenkins.io/sonar-quality-gates/

![Jenkins-插件管理-1](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/Jenkins-插件管理-1.png)

![Jenkins-插件管理-2](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/Jenkins-插件管理-2.png)

### Jenkins-凭据设置

> 添加 SonarQube、GitLab创建的用户令牌 Token 与 钉钉机器人创建的Webhook URL（含Token）

![Jenkins-系统管理-凭据设置-1](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/Jenkins-系统管理-凭据设置-1.png)

![Jenkins-系统管理-凭据设置-2](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/Jenkins-系统管理-凭据设置-2.png)

### Jenkins-系统管理-系统设置-Sonar Quality Gates&GitLab

![Jenkins-系统管理-系统设置-Sonar Quality Gates&GitLab](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/Jenkins-系统管理-系统设置-Sonar Quality Gates&GitLab.png)

### Jenkins-Pipeline JOB 设置GitLab Trigger

![Jenkins-Pipeline JOB 设置GitLab Trigger](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/Jenkins-Pipeline JOB 设置GitLab Trigger.png)

#### Jenkins Pipeline

- [gitlab-sonarqube-webhook-trigge.jenkinsfile](devops-library\projects\gitlab-sonarqube-webhook-trigge.jenkinsfile)

```groovy
pipeline {
    agent { label "build" }

    options {
        skipDefaultCheckout true
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '180', numToKeepStr: '90')
    }

    environment {
        gitlabKeysCredentialsId = "a7d76450-d876-44a8-8d96-92f11cd013b0"
        sonarqubeUserTokenCredentialsId = "c23d40dd-a6c8-4a17-a0d1-23dd795fe773"
        sonarqubeHostUrl = "http://192.168.100.150:9000/sonarqube"
        dingTalkTokenCredentialsId = "8c6083c7-e1c2-47c0-9367-b67a9469bcd5"
        dingTalkRebotIdCredentialsId = "7a711aa6-04b5-4a6d-9907-c0f6b90f6acc"
        gitlabUserTokenCredentialsId = "36e10c3d-997d-4eaa-9e46-d9848d5d6631"
        buName = "devops"
    }

    parameters {
        string(name: 'gitlabSourceBranch', defaultValue: '', description: 'GitLab Source Branch')
        string(name: 'gitlabSourceRepoName', defaultValue: '', description: 'GitLab Source Repo Name')
    }

    stages {
        stage("Global") {
            steps {
                script {
                    currentBuild.description = """ gitlab-webhook-trigger Code Scan Projects \n [SonarQube：${env.sonarqubeHostUrl}](${env.sonarqubeHostUrl}) """
                }
            }
        }

        stage("CodeScan") {
            steps {
                script {
                    def branchName = params.gitlabSourceBranch ?: env.gitlabSourceBranch
                    def repoName = params.gitlabSourceRepoName ?: env.gitlabSourceRepoName
                    def projectKey = "${env.buName}-${repoName}"

                    // 获取项目ID和Commit ID
                    def projectId = gitlab.GetProjectId("${env.gitlabUserTokenCredentialsId}", "${env.buName}", "${repoName}")
                    def commitId = gitlab.GetCommitIdByApi("${env.gitlabUserTokenCredentialsId}", "${projectId}", "${branchName}")

                    // 创建工作目录并执行代码扫描
                    sh "[ -d ${projectId} ] || mkdir ${projectId}"
                    ws("${WORKSPACE}/${projectId}") {
                        def srcUrl = "192.168.100.150:${env.buName}/${repoName}.git"
                        checkout.GetCode(srcUrl, branchName, "${env.gitlabKeysCredentialsId}")
                        codeScan.CodeScan_Sonar("${env.sonarqubeUserTokenCredentialsId}", "${env.gitlabUserTokenCredentialsId}", branchName, commitId, projectId)
                        codeScan.SonarQubeMetricsAndNotify("${env.sonarqubeHostUrl}", projectKey, "${env.sonarqubeUserTokenCredentialsId}", "${env.dingTalkRebotIdCredentialsId}")
                    }
                }
            }
        }

        stage('Check Quality Gate') {
            steps {
                script {
                    def qualityGate = waitForQualityGate()
                    if (qualityGate.status != 'OK') {
                        error "Pipeline failed due to quality gate failure: ${qualityGate.status}"
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            script {
                updateGitlabCommitStatus name: 'sonarqube', state: 'success', description: 'SonarQube Quality Gate passed'
                notice.dingTalkPluginNotice("${env.dingTalkRebotIdCredentialsId}")
            }
        }
        failure {
            script {
                updateGitlabCommitStatus name: 'sonarqube', state: 'failed', description: 'SonarQube Quality Gate failed'
                notice.dingTalkPluginNotice("${env.dingTalkRebotIdCredentialsId}")
            }
        }
    }
}
```

- [CodeScan.groovy](devops-library\src\org\devops\CodeScan.groovy)

```groovy
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
 */
def CodeScan_Sonar(sonarqubeUserTokenCredentialsId, gitlabUserTokenCredentialsId, projectVersion, commitId, projectId) {
    cliPath = "/opt/sonar-scanner/bin"
    withCredentials([string(credentialsId: "${sonarqubeUserTokenCredentialsId}", variable: 'SONARQUBE_USER_TOKEN'),
                     string(credentialsId: "${gitlabUserTokenCredentialsId}", variable: 'GITLAB_USER_TOKEN')]) {
        // 远程构建时推荐使用CommitID作为代码扫描-项目版本
        sh """
            ${cliPath}/sonar-scanner \
            -Dsonar.login=${SONARQUBE_USER_TOKEN} \
            -Dsonar.projectVersion=${projectVersion} \
            -Dsonar.branch.name=${projectVersion} \
            -Dsonar.gitlab.commit_sha=${commitId} \
            -Dsonar.gitlab.ref_name=${projectVersion} \
            -Dsonar.gitlab.project_id=${projectId} \
            -Dsonar.gitlab.user_token=${GITLAB_USER_TOKEN}
        """
    }
}


```

- [Notice.groovy](devops-library\src\org\devops\Notice.groovy)

```groovy
/**
 * SonarQube指标&通知
 * @param sonarHostUrl SonarQube访问地址
 * @param projectKey SonarQube项目key
 * @param sonarqubeUserTokenCredentialsId SonarQube用户Token凭据ID
 * @param dingTalkRobotIdCredentialsId 钉钉机器人Token凭据ID
 */
def SonarQubeMetricsAndNotify(sonarHostUrl, projectKey, sonarqubeUserTokenCredentialsId, dingTalkRobotIdCredentialsId) {
    withCredentials([string(credentialsId: "${sonarqubeUserTokenCredentialsId}", variable: 'SONARQUBE_USER_TOKEN'),
                     string(credentialsId: "${dingTalkRobotIdCredentialsId}", variable: 'DINGTALK_ROBOT_ID')]) {

        // 获取 SonarQube 扫描结果
        def json = sh(script: """
            curl -u ${SONARQUBE_USER_TOKEN}: "${sonarHostUrl}/api/measures/component?component=${projectKey}&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density"
        """, returnStdout: true).trim()

        // 解析 JSON 数据
        def jsonObject = readJSON(text: json)
        def metrics = ['bugs', 'vulnerabilities', 'code_smells', 'coverage', 'duplicated_lines_density'].collectEntries {
            [(it): jsonObject.component.measures.find { measure -> measure.metric == it }?.value ?: 'N/A']
        }

        // 颜色逻辑：如果关键字段为 0，显示绿色；如果大于 0，显示红色
        def bugColor = "${metrics.bugs}".toInteger() > 0 ? 'red' : 'green'
        def vulnerabilityColor = "${metrics.vulnerabilities}".toInteger() > 0 ? 'red' : 'green'
        def codeSmellColor = "${metrics.code_smells}".toInteger() > 0 ? 'orange' : 'green'
        // 假设 80% 覆盖率为合格标准
        def coverageColor = "${metrics.coverage}".toDouble() < 80.0 ? 'orange' : 'green'
        // 假设 10% 重复率为警戒线
        def duplicatedLinesColor = "${metrics.duplicated_lines_density}".toDouble() > 10.0 ? 'red' : 'green'

        // 构建通知消息
        def message = [
                "### SonarQube 扫描结果 - 项目：${projectKey}",
                "- **Bugs**: <font color=${bugColor}>${metrics.bugs}</font>",
                "- **Vulnerabilities**: <font color=${vulnerabilityColor}>${metrics.vulnerabilities}</font>",
                "- **Code Smells**: <font color=${codeSmellColor}>${metrics.code_smells}</font>",
                "- **Coverage**: <font color=${coverageColor}>${metrics.coverage}%</font>",
                "- **Duplicated Lines Density**: <font color=${duplicatedLinesColor}>${metrics.duplicated_lines_density}%</font>"
        ]

        // 发送钉钉通知
        dingtalk robot: "${DINGTALK_ROBOT_ID}",
                type: "MARKDOWN",
                title: "SonarQube 扫描通知 - ${projectKey}",
                text: message,
                atAll: false
    }
}
```

## 钉钉

### 钉钉群聊添加自定义机器人-webhook

![钉钉群聊添加自定义机器人-webhook](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/钉钉群聊添加自定义机器人-webhook.png)

### Jenkins-系统管理-钉钉插件设置

> 注意：钉钉机器人的ID保存至Jenkins凭据，Jenkins Pipeline 使用凭据ID调用机器人

![Jenkins-系统管理-钉钉插件设置](Jenkins集成GitLab、SonarQube通过Pipeline实现代码提交、请求合并触发CICD.assets/Jenkins-系统管理-钉钉插件设置.png)

