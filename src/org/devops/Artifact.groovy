package org.devops

/**
 * Nexus Api上传制品
 * @param directory 目录
 * @param filePath 文件路径
 * @param fileName 文件名称
 * @return
 */
def PushArtifactByApi(directory, filePath, fileName) {
    sh """
        curl -X POST "http://192.168.20.194:8081/service/rest/v1/components?repository=devops-local" \\
            -H "accept: application/json" \\
            -H "Content-Type: multipart/form-data" \\
            -F "raw.directory=${directory}" \\
            -F "raw.asset1=@${filePath}/${fileName};type=application/java-archive" \\
            -F "raw.asset1.filename=${fileName}" \\
            -u admin:proaim@2013
    """
}

/**
 * Nexus插件上传制品
 * 插件链接：https://plugins.jenkins.io/nexus-artifact-uploader
 * @param artifactId 制品id
 * @param file 制品文件
 * @param type 文件类型（jar、zip）
 * @param groupId 组id
 * @param repository 仓库名称
 * @param version 制品版本号
 */
def PushArtifactByNexusPlugin(artifactId, file, type, groupId, repository, version) {
    nexusArtifactUploader artifacts: [[artifactId: artifactId,
                                       classifier: '',
                                       file      : file,
                                       type      : type]],
            credentialsId: '55c0f9ca-e3a4-4eee-a59d-14baf5344a28',
            groupId: groupId,
            nexusUrl: '192.168.20.194:8081',
            nexusVersion: 'nexus3',
            protocol: 'http',
            repository: repository,
            version: version
}

/**
 * Maven Cli上传制品
 * @param artifactId 制品id
 * @param file 制品文件
 * @param type 文件类型（jar、zip）
 * @param groupId 组id
 * @param repository 仓库名称
 * @param version 制品版本号
 * @param repositoryId 对应Maven setting.xml配置文件 server标签下的id标签（认证）
 */
def PushArtifactByMavenCli(artifactId, file, type, groupId, repository, version) {
    sh """
        mvn deploy:deploy-file \
            -DartifactId=${artifactId} \
            -Dfile=${file} \
            -Dpackaging=${type} \
            -DgroupId=${groupId} \
            -Durl=http://192.168.20.194:8081/repository/${repository} \
            -Dversion=${version} \
            -DrepositoryId=nexus-local-auth
    """
}

/**
 * Maven Cli上传制品（pom.xml）
 * @param file 制品文件
 * @param repository 仓库名称
 * @param repositoryId 对应Maven setting.xml配置文件 server标签下的id标签（认证）
 */
def PushArtifactByMavenCliAndPom(file, repository) {
    sh """
        mvn deploy:deploy-file \
            -Dfile=${file} \
            -DpomFile=pom.xml \
            -Durl=http://192.168.20.194:8081/repository/${repository} \
            -DrepositoryId=nexus-local-auth
    """
}

/**
 * Nexus Api下载制品
 * @param filePath 文件路径
 * @param fileName 文件名称
 * @return
 */
def PullArtifactByApi(filePath, fileName) {
    sh """
        curl http://192.168.20.194:8081/repository/devops-local/${filePath}/${fileName} \
        -u admin:proaim@2013 \
        -o ${fileName} -s
    """
}