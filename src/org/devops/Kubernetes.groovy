package org.devops

/**
 * Kubernetes发布模板文件内容替换并转换Base64（CI）
 * @param fileName 模板文件名称
 * @param fileData 模板文件源内容
 * @param imagePath 镜像仓库地址/镜像名称:镜像标签
 */
def K8sReleaseTemplateFileReplaceAndConvertToBase64(fileName, fileData, imagePath) {
    // 删除本地旧文件
    sh "rm -fr ${fileName}"
    // 根据文件内容生成新文件
    writeFile file: "${fileName}", text: "${fileData}"

    // 替换模板文件内容
    sh """
        sed -i 's#__IMAGE_NAME__#${imagePath}#g' ${fileName}
        #cat ${fileName}
    """

    // 获取标准输出返回内容（获取替换后的模板文件内容，封装到Groovy变量中）
    newYaml = sh returnStdout: true, script: "cat ${fileName}"
    // 转换文件内容编码格式：Base64
    return "${newYaml}".bytes.encodeBase64().toString()
}

/**
 * Kubernetes发布模板文件内容替换（CD）
 * @param fileName 模板文件名称
 * @param fileData 模板文件源内容
 * @param domainName 访问域名
 * @param port 端口号
 * @param serviceName 服务/项目名称
 * @param namespace 命名空间
 */
def K8sReleaseTemplateFileReplace(fileName, fileData, domainName, port, serviceName, namespace) {
    // 删除本地旧文件
    sh "rm -fr ${fileName}"
    // 根据文件内容生成新文件
    writeFile file: "${fileName}", text: "${fileData}"

    // 替换模板文件内容
    sh """
        sed -i 's#__DOMAIN_NAME__#${domainName}#g' ${fileName}
        sed -i 's#__SERVICE_PORT__#${port}#g' ${fileName}
        sed -i 's#__APP_NAME__#${serviceName}#g' ${fileName}
        sed -i 's#__NAMESPACE__#${namespace}#g' ${fileName}
        cat ${fileName}
    """
}

/**
 * Kubernetes - Kubectl部署应用
 * @param namespace 命名空间
 * @param deployFileName 部署文件名称
 * @param serviceName 服务/项目名称
 */
def KubernetesDeploy(namespace, deployFileName, serviceName) {
    // 发布应用至Kubernetes
    sh """
        kubectl apply -f ${deployFileName}
    """

    // 获取应用状态
    5.times {
        sh "sleep 2; kubectl -n ${namespace} get pod | grep ${serviceName}"
    }
}

/**
 * Kubernetes-Helm发布模板文件内容替换并转换Base64（CI）
 * @param fileName 模板文件名称
 * @param fileData 模板文件源内容
 * @param imagePath 镜像仓库地址/镜像名称:镜像标签
 * 注意：writeYaml data: yamlData 变量值不可以加"${yamlData}"，否则会丢失换行符且文本开头及末尾增加单引号：'
 */
def HelmReleaseTemplateFileReplaceAndConvertToBase64(fileName, fileData, imageName, imageTag) {
    // 删除本地旧文件
    sh "rm -fr ${fileName}"
    // 替换模板文件内容
    yamlData = readYaml text: "${fileData}"
    yamlData.image.repository = "${imageName}"
    yamlData.image.tag = "${imageTag}"
    // 根据文件内容生成新文件
    writeYaml charset: "UTF-8", overwrite: "true", file: "${fileName}", data: yamlData
    sh "cat ${fileName}"

    // 获取标准输出返回内容（获取替换后的模板文件内容，封装到Groovy变量中）
    newYaml = sh returnStdout: true, script: "cat ${fileName}"
    // 转换文件内容编码格式：Base64
    return "${newYaml}".bytes.encodeBase64().toString()
}

/**
 * Kubernetes发布模板文件内容替换（CD）
 * @param filePath Git模板文件路径：项目服务名称_Helm/values.yaml
 * @param domainName 服务访问域名
 * @param accessDomainName 应用访问域名（前后端分离项目）
 * @param memory 最大内存
 * @param replicaCount 副本数
 * @param projectParamsMap 项目参数
 * 注意：writeYaml data: yamlData 变量值不可以加"${yamlData}"，否则会丢失换行符且文本开头及末尾增加单引号：'
 */
def HelmReleaseTemplateFileReplace(filePath, domainName, accessDomainName, memory, replicaCount, Map projectParamsMap) {
    // 替换模板文件内容
    yamlData = readYaml file: "${filePath}"
    yamlData.ingress.hosts[0].host = "${domainName}"
    if (projectParamsMap != null) {
        //yamlData.service[0].port = "${port}"
        /*for (projectParams in projectParamsMap) {
            yamlData."${projectParams.key}" = "${projectParams.value}"
        }*/
        projectParamsMap.each { key, value -> yamlData."${key}" = "${value}" }
    }
    if (domainName != accessDomainName) {
        println("accessDomainName Updates！")
        yamlData.ingress.hosts[1].host = "${accessDomainName}"
    }

    yamlData.resources.limits.memory = "${memory}"
    yamlData.replicaCount = "${replicaCount}"

    // 根据文件内容生成新文件
    writeYaml charset: "UTF-8", overwrite: "true", file: "${filePath}", data: yamlData
    sh "cat ${filePath}"
}

/**
 * Kubernetes - Helm部署应用
 * @param namespace 命名空间
 * @param deployFilePath 部署文件路径（Helm Chart）
 * @param serviceName 服务/项目名称
 */
def HelmDeploy(namespace, deployFilePath, serviceName) {
    // Helm发布应用至Kubernetes
    sh """
        pwd
        helm package "${deployFilePath}"
        helm upgrade --install --create-namespace "${serviceName}" "${serviceName}"-*.tgz -n ${namespace}
        helm history "${serviceName}" -n ${namespace}
    """

    //获取release的历史版本
    env.revision = sh returnStdout: true, script: """ helm history ${serviceName} -n ${namespace} | grep -v 'REVISION' | awk '{print \$1}' """
    println("${env.revision}".split('\n').toString())
    env.REVISION = "${env.revision}".split('\n').toString()
    println("${env.REVISION}")

    // 获取应用状态
    5.times {
        sh "sleep 2; kubectl -n ${namespace} get pod | grep ${serviceName}"
    }
}