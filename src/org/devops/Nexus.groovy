package org.devops

/**
 * Nexus插件上传制品
 * 插件链接：https://plugins.jenkins.io/nexus-artifact-uploader
 * @param artifactId 制品id
 * @param file 制品文件
 * @param type 文件类型（jar、zip）
 * @param groupId 组id
 * @param repository 仓库名称
 * @param version 制品版本号
 * @return 制品URL
 */
def PushArtifactByNexusPlugin(artifactId, file, type, groupId, repository, version) {
    nexusArtifactUploader artifacts: [[artifactId: artifactId,
                                       classifier: '',
                                       file      : file,
                                       type      : type]],
            credentialsId: '55c0f9ca-e3a4-4eee-a59d-14baf5344a28',
            groupId: groupId,
            nexusUrl: '192.168.20.197:8081',
            nexusVersion: 'nexus3',
            protocol: 'http',
            repository: repository,
            version: version
}