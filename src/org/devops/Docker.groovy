package org.devops

/**
 * Docker构建并上传镜像
 * @param registry 镜像仓库地址
 * @param credentialsId 镜像仓库访问凭据Id
 * @param imageName 镜像名称（推荐定义："${buName}/${serviceName}"）
 * @param imageTag 镜像标签（推荐定义："${branchName}-${commitId}"）
 * @param filePath 文件路径
 * @param fileName 文件名称（推荐定义："${serviceName}-${version}.${fileSuffix}"）
 * Docker（/etc/docker/daemon.json）配置Harbor私有仓库："insecure-registries":["http://192.168.100.150:8082", "https://harbor.devops.com"],
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

            # 保存镜像
            docker save ${registry}/${imageName}:${imageTag} -o ${env.serviceName}:${imageTag}.tar
            
            # 删除镜像
            sleep 5
            docker rmi ${registry}/${imageName}:${imageTag}
            
            # 删除dangling镜像
            danglingImages=\$(docker images -f "dangling=true" -q)
            if [ -z \${danglingImages} ]; then
                echo "No hanging images found."
            else
                echo "Removing hanging images..."
                docker rmi \${danglingImages}
            fi
        """
    }

}
