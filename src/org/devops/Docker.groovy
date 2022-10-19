package org.devops

/**
 * Docker构建并上传镜像
 * @param registry 镜像仓库地址
 * @param credentialsId 镜像仓库访问凭据Id
 * @param imageName 镜像名称（推荐定义："${buName}/${serviceName}"）
 * @param imageTag 镜像标签（推荐定义："${branchName}-${commitId}"）
 * @param filePath 文件路径
 * @param fileName 文件名称（推荐定义："${serviceName}-${version}.${fileSuffix}"）
 */
def DockerBuildAndPushImage(registry, credentialsId, imageName, imageTag, filePath, fileName) {
    withCredentials([usernamePassword(credentialsId: "${credentialsId}",
            usernameVariable: 'HARBOR_USERNAME',
            passwordVariable: 'HARBOR_PASSWORD')]) {
        // docker login -u ${HARBOR_USERNAME} -p ${HARBOR_PASSWORD} ${registry}
        sh """
            # 登录镜像仓库
            echo "${HARBOR_PASSWORD}" | docker login -u ${HARBOR_USERNAME} --password-stdin ${registry}
            
            # 构建镜像
            docker build -t ${registry}/${imageName}:${imageTag} . --build-arg filePath=${filePath} --build-arg fileName=${fileName}
            
            # 上传镜像
            docker push ${registry}/${imageName}:${imageTag}
            
            # 删除镜像
            sleep 5
            docker rmi ${registry}/${imageName}:${imageTag}
            # docker rmi \$(docker images -f "dangling=true" -q)
        """
    }

}
