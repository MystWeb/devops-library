package org.devops

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