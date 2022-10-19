package org.devops

/**
 * Nexus Api上传制品
 * @param registry 制品仓库地址
 * @param credentialsId 访问凭据Id
 * @param repository 制品仓库名称
 * @param directory 目录（推荐定义：/buName/serviceName/branch-version/serviceName-version.suffix）
 * @param filePath 文件路径
 * @param fileName 文件名称
 */
def PushArtifactByApi(registry, credentialsId, repository, directory, filePath, fileName) {
    withCredentials([usernamePassword(credentialsId: "${credentialsId}",
            usernameVariable: 'NEXUS_USERNAME',
            passwordVariable: 'NEXUS_PASSWORD')]) {
        sh """
            curl -X POST "http://${registry}/service/rest/v1/components?repository=${repository}" \\
                -H "accept: application/json" \\
                -H "Content-Type: multipart/form-data" \\
                -F "raw.directory=${directory}" \\
                -F "raw.asset1=@${filePath}/${fileName};type=application/java-archive" \\
                -F "raw.asset1.filename=${fileName}" \\
                -u "${NEXUS_USERNAME}":"${NEXUS_PASSWORD}"
        """
    }
}

/**
 * Nexus插件上传制品
 * 插件链接：https://plugins.jenkins.io/nexus-artifact-uploader
 * @param registry 制品仓库地址
 * @param credentialsId 访问凭据Id
 * @param artifactId 制品id
 * @param file 制品文件
 * @param type 文件类型（jar、zip）
 * @param groupId 组id
 * @param repository 仓库名称
 * @param version 制品版本号
 */
def PushArtifactByNexusPlugin(registry, credentialsId, artifactId, file, type, groupId, repository, version) {
    nexusArtifactUploader artifacts: [[artifactId: artifactId,
                                       classifier: '',
                                       file      : file,
                                       type      : type]],
            credentialsId: "${credentialsId}",
            groupId: groupId,
            nexusUrl: "${registry}",
            nexusVersion: 'nexus3',
            protocol: 'http',
            repository: repository,
            version: version
}

/**
 * Maven Cli上传制品
 * @param registry 制品仓库地址
 * @param artifactId 制品id
 * @param file 制品文件
 * @param type 文件类型（jar、zip）
 * @param groupId 组id
 * @param repository 仓库名称
 * @param version 制品版本号
 * @param repositoryId 对应Maven setting.xml配置文件 server标签下的id标签（认证）
 */
def PushArtifactByMavenCli(registry, artifactId, file, type, groupId, repository, repositoryId, version) {
    sh """
        mvn deploy:deploy-file \
            -DartifactId=${artifactId} \
            -Dfile=${file} \
            -Dpackaging=${type} \
            -DgroupId=${groupId} \
            -Durl=http://${registry}/repository/${repository} \
            -Dversion=${version} \
            -DrepositoryId=${repositoryId}
    """
}

/**
 * Maven Cli上传制品（pom.xml）
 * @param registry 制品仓库地址
 * @param file 制品文件
 * @param repository 制品仓库名称
 * @param repositoryId 对应Maven setting.xml配置文件 server标签下的id标签（认证）
 */
def PushArtifactByMavenCliAndPom(registry, file, repository, repositoryId) {
    sh """
        mvn deploy:deploy-file \
            -Dfile=${file} \
            -DpomFile=pom.xml \
            -Durl=http://${registry}/repository/${repository} \
            -DrepositoryId=${repositoryId}
    """
}

/**
 * Nexus Api下载制品
 * @param registry 制品仓库地址
 * @param credentialsId 访问凭据Id
 * @param repository 制品仓库名称
 * @param filePath 文件路径
 * @param fileName 文件名称
 * @return
 */
def PullArtifactByApi(registry, credentialsId, repository, filePath, fileName) {
    withCredentials([usernamePassword(credentialsId: "${credentialsId}",
            usernameVariable: 'NEXUS_USERNAME',
            passwordVariable: 'NEXUS_PASSWORD')]) {
        sh """
            curl http://${registry}/repository/${repository}/${filePath}/${fileName} \
            -u "${NEXUS_USERNAME}":"${NEXUS_PASSWORD}" \
            -o ${fileName} -s
        """
    }
}