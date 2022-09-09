// 加载共享库
@Library("mylib@main")

import org.devops.Harbor

// New实例化
def harbor = new Harbor()
pipeline {

    agent {
        label "build"
    }

    parameters {
        choice choices: ['devops-maven-service'], description: '注意：选择仓库', name: 'repoName'
    }

    stages {
        stage("GetTags") {
            steps {
                script {
                    // Harbor项目名称/GitLab组名称
                    env.projectName = "${env.repoName}".split("-")[0]
                    env.result = harbor.GetArtifactTag("${env.projectName}", "${env.repoName}")
                    env.result = env.result - '[' - ']'
                }
            }
        }


        stage("Clean") {
            steps {
                script {
                    def result = input message: "是否删除${env.projectName}项目的${env.repoName}这些标签：",
                            parameters: [extendedChoice(defaultValue: "${env.result}",
                                    multiSelectDelimiter: ',',
                                    name: 'tags',
                                    quoteValue: false,
                                    saveJSONParameterToFile: false,
                                    type: 'PT_CHECKBOX',
                                    value: "${env.result}",
                                    visibleItemCount: 20)]
                    println("${result}")
                    // println("Delete  ${taga}, doing.......")
                    // tags = "${taga}" - '[' - ']'

                    for (t in result.split(',')) {
                        println("Delete >>>>" + t.trim())
                        harbor.DeleteArtifactTag(env.projectName, env.repoName, t.trim())
                    }
                }
            }
        }

    }
}