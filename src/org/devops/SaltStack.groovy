package org.devops

/**
 * SaltStack 多主机远程发布（推荐Ansible）
 * 1、所有节点执行：sudo rpm --import https://repo.saltproject.io/py3/redhat/7/x86_64/3004/SALTSTACK-GPG-KEY.pub \
 * curl -fsSL https://repo.saltproject.io/py3/redhat/7/x86_64/3004.repo | sudo tee /etc/yum.repos.d/salt.repo \
 * sudo yum clean expire-cache
 * 2、Jenkins Agent节点安装slat-master：sudo yum install salt-master
 * 3、主机节点安装slat-minion：sudo yum install salt-minion
 * 4、slat-minion节点申请加入slat-master：vim /etc/salt/minion
 * 新增写入内容：master: 192.168.20.194
 * 5、slat-master节点查看并同意所有slat-minion加入：salt-key -L && salt-key -A
 * 6、主机节点列表安装运行环境：yum -y install java-11-openjdk java-11-openjdk-devel
 * @param targetHosts 远程发布主机列表
 * @param targetDir 远程发布主机目录
 * @param serviceName 服务/项目名称
 * @param version 服务版本号（推荐定义："${branchName}-${commitId}"）
 * @param fileName 文件名称
 * @param port 服务监听的端口号
 */
def SaltStackDeploy(targetHosts, targetDir, serviceName, version, fileName, port) {
    // 文件存放目录
    localDeployDir = "/srv/salt/${serviceName}"

    // 复制文件到主机
    sh """
        # 检查目录是否存在
        [ -d ${localDeployDir} ] || mkdir -p ${localDeployDir}
        mv ${fileName} ${localDeployDir}

        # 清理和创建发布目录
        salt -L "${targetHosts}" cmd.run "rm -fr ${targetDir}/${serviceName}/* \\
            && mkdir -p ${targetDir}/${serviceName} || echo file is exists"

        # 复制应用文件
        salt -L "${targetHosts}" cp.get_file salt://${serviceName}/${fileName} ${targetDir}/${serviceName}/
    """

    // 获取共享库资源文件内容（String字符串） - 发布脚本
    fileData = libraryResource 'scripts/service.sh'
    // 将共享库资源文件内容写入文件
    writeFile file: 'service.sh', text: "${fileData}"
    sh "ls -a ; cat service.sh"

    // 启动应用服务
    sh """
        # 复制脚本
        mv service.sh ${localDeployDir}
        salt -L "${targetHosts}" cp.get_file salt://${serviceName}/service.sh ${targetDir}/${serviceName}/

        # 启动服务
        salt -L "${targetHosts}" cmd.run "cd ${targetDir}/${serviceName} ; source /etc/profile \\
        && sh service.sh ${serviceName} ${version} ${port} start"

        # 检查服务
        sleep 5
        salt -L "${targetHosts}" cmd.run "cd ${targetDir}/${serviceName} ; source /etc/profile \\
        && sh service.sh ${serviceName} ${version} ${port} check"
    """
}
