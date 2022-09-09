package org.devops

/**
 * 插件链接：https://plugins.jenkins.io/extended-choice-parameter
 * 1、获取镜像列表
 * 2、用户选择删除
 * 3、调用api删除
 *
 * Json解析器：import groovy.json.JsonSlurper
 */

/**
 * 删除镜像tag
 * @param projectName Harbor项目名称/GitLab组名称
 * @param repoName Harbor仓库名称
 * @param tagName 标签名称
 * @return
 */
def DeleteArtifactTag(projectName, repoName, tagName) {
    harborAPI = "http://192.168.20.194:8088/api/v2.0/projects/${projectName}/repositories/${repoName}"
    apiURL = "artifacts/${tagName}/tags/${tagName}"
    sh """ curl -X DELETE "${harborAPI}/${apiURL}" -H "accept: application/json"  -u admin:Harbor12345 """
}

/**
 * 获取镜像的所有标签
 * 例：devops-maven-service
 * @param projectName Harbor项目名称/GitLab组名称
 * @param repoName Harbor仓库名称
 * @return 标签列表
 * TODO：如果Harbor存在空标签镜像，需要删除空标签镜像，否则出现Exception：groovy.lang.MissingPropertyException: No such property: name for class: net.sf.json.JSONNull
 */
def GetArtifactTag(projectName, repoName) {
    harborAPI = "http://192.168.20.194:8088/api/v2.0/projects/${projectName}/repositories/${repoName}"
    apiURL = "artifacts?page=1&page_size=10"
    response = sh returnStdout: true, script: """curl -X GET "${harborAPI}/${apiURL}" -H "accept: application/json" -u admin:Harbor12345 """
    if ("" != "${response}" || "${response}".trim().length() > 0) {
        response = readJSON text: """ ${response - "\n"} """
    } else {
        response = readJSON text: """{"errors" : true}"""
    }
    tags = []
    for (t in response[0].tags) {
        tags << t.name
    }

    return tags
}