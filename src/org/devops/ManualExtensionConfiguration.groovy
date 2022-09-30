package org.devops

/**
 * Active Choices 动态关联参数
 * 插件链接：https://plugins.jenkins.io/uno-choice/
 * 注意：无法与parameters {} 同存，建议在Jenkins手动增加配置
 */
def ActiveChoices() {
    properties([
            parameters([
                    text(defaultValue: 'RELEASE-1.1.1', description: '注意：选择发布分支', name: 'branchName'),
                    choice(choices: ['jar', 'war', 'html', 'go', 'py'], description: '注意：选择制品类型', name: 'artifactType'),
                    [$class      : "ChoiceParameter",
                     choiceType  : "PT_SINGLE_SELECT",
                     description : "注意：选择发布环境",
                     filterLength: 1,
                     filterable  : false,
                     name        : "envList",
                     randomName  : "choice-parameter-27203637064885",
                     script      : [
                             $class        : "GroovyScript",
                             fallbackScript: [
                                     classpath: [],
                                     sandbox  : false,
                                     script   :
                                             '''return["Could not get EnvList"]'''
                             ],
                             script        : [
                                     classpath: [],
                                     sandbox  : true,
                                     script   :
                                             '''return ["dev", "uat", "stag", "prod"]'''
                             ]
                     ]
                    ],
                    [$class              : "CascadeChoiceParameter",
                     choiceType          : "PT_SINGLE_SELECT",
                     description         : "注意：选择远程发布主机",
                     filterLength        : 1,
                     filterable          : false,
                     name                : "targetHosts",
                     randomName          : "choice-parameter-27203638641714",
                     referencedParameters: "envList",
                     script              : [
                             $class        : "GroovyScript",
                             fallbackScript: [
                                     classpath: [],
                                     sandbox  : false,
                                     script   :
                                             '''return["Could not get Environment from Env Param"]'''
                             ],
                             script        : [
                                     classpath: [],
                                     sandbox  : true,
                                     script   :
                                             '''
                          switch (envList) {
                              case "dev":
                                  return ["192.168.20.158"]
                                  break
                              case "uat":
                                  return ["192.168.20.191"]
                                  break
                              case "stag":
                                  return ["node01"]
                                  break
                              case "prod":
                                  return ["node02"]
                                  break
                              default:
                                  break
                          }
                        '''
                             ]
                     ]
                    ],
                    choice(choices: ['/opt', '/tmp'], description: '注意：选择远程主机的发布目录', name: 'targetDir'),
                    text(defaultValue: '8090', description: '注意：服务监听的端口号', name: 'port')
            ])
    ])
}