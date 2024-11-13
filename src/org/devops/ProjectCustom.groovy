package org.devops

/**
 * 根据服务名称执行对应的构建工具和版本
 * @param serviceName 服务名称
 */
def executeBuildByServiceName(serviceName) {
    def build = new Build()

    // 服务名称与构建操作的映射
    def serviceBuildMap = [
            "devops-web-be": { build.MavenBuildByJDKVersion("21") },
            "devops-web-fe": { build.NpmBuildByVersion("nodejs-18") }
    ]

    // 获取对应服务的构建操作并执行
    def buildAction = serviceBuildMap[serviceName]
    // 获取 buildAction 后增加了错误处理，确保在找不到对应服务名称时给出明确的错误信息。
    if (buildAction) {
        buildAction()
    } else {
        error "No build action defined for service: ${serviceName}"
    }
}

/**
 * 获取项目后端Java启动参数
 * @param envList 环境列表
 * @return devops应用后端Java启动参数
 */
static def getProjectParamsMap(envList) {
    def map = [
            "dev01" : [
                    "PARAMS": "--spring.config.additional-location=classpath:/config/devops/ \
                        --spring.profiles.active=dev1"
            ],
            "dev02" : [
                    "PARAMS": "--spring.config.additional-location=classpath:/config/devops/ \
                        --spring.profiles.active=dev2"
            ],
            "test01": [
                    "PARAMS": "--spring.config.additional-location=classpath:/config/devops/ \
                        --spring.profiles.active=test1"
            ],
            "test02": [
                    "PARAMS": "--spring.config.additional-location=classpath:/config/devops/ \
                        --spring.profiles.active=test2"
            ]
    ]
    return map["${envList}"]
}

/**
 * 获取定制化Git项目的projectId
 * 如：GitLab项目名称：devops/devops-web-backend（标准化），devops/devops01-demo（非规范化）
 * @param projectName 项目名称
 * @return 非规范化Git项目的projectId
 */
static def getProjectIdByProjectName(projectName) {
    switch (projectName) {
        case "devops-web-backend":
            return 41
            break
        default:
            break
    }
}

/**
 * 获取前后端分离项目的前端访问域名
 * 如：GitLab项目名称：devops/devops-web-backend（标准化），devops/devops01-demo（非规范化）
 * @param domainName 后端服务的域名
 * @return 前端访问域名
 */
static def getAccessDomainName(domainName) {
    switch (domainName) {
        case "devops.web.backend":
            return "devops.web.frontend"
            break
        default:
            break
    }
}