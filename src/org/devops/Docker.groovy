package org.devops

/**
 * Docker构建并上传镜像
 * @param imageName 镜像名称
 * @param imageTag 镜像标签
 */
def DockerBuildAndPushImage(imageName, imageTag) {
    withCredentials([usernamePassword(credentialsId: 'ef5a1de1-0840-4b51-a0b0-dc04f98544f3',
            usernameVariable: 'HARBOR_USERNAME',
            passwordVariable: 'HARBOR_PASSWORD')]) {
            // docker login -u ${HARBOR_USERNAME} -p ${HARBOR_PASSWORD} 192.168.20.194:8088
        sh """
            # 登录镜像仓库
            echo "${HARBOR_PASSWORD}" | docker login -u ${HARBOR_USERNAME} --password-stdin 192.168.20.194:8088
            
            # 构建镜像
            docker build -t 192.168.20.194:8088/${imageName}:${imageTag} .
            
            # 上传镜像
            docker push 192.168.20.194:8088/${imageName}:${imageTag}
            
            # 删除镜像
            sleep 5
            docker rmi 192.168.20.194:8088/${imageName}:${imageTag}
            docker rmi \$(docker images -f "dangling=true" -q)
        """
    }

}
