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
        kubectl -n ${namespace} apply -f ${deployFileName}
    """

    // 获取应用状态
    5.times {
        sh "sleep 2; kubectl -n ${namespace} get pod | grep ${serviceName}"
    }
}