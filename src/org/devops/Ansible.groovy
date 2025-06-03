package org.devops

/**
 * Ansible 多主机远程发布
 * 1、Jenkins Agent节点、主机节点列表都需安装ansible：yum -y install epel-release ansible
 * 2、Jenkins Agent节点与主机节点列表实现免密登录：ssh-keygen -t rsa && ssh-copy-id root@localhost
 * 3、主机节点列表安装运行环境：yum -y install java-11-openjdk java-11-openjdk-devel
 * @param targetHosts 远程发布主机列表
 * @param targetDir 远程发布主机目录
 * @param serviceName 服务/项目名称
 * @param version 服务版本号（推荐定义："${branchName}-${commitId}"）
 * @param fileName 文件名称
 * @param port 服务监听的端口号
 * 插件链接：https://plugins.jenkins.io/extended-choice-parameter/
 */
def AnsibleDeploy(targetHosts, targetDir, serviceName, version, fileName, port) {
    // 删除旧的hosts
    sh "rm -fr hosts"
    // 根据选择的发布主机生成hosts
    for (final def host in "${targetHosts}".split(',')) {
        sh "echo ${host} >> hosts"
    }
    sh "cat hosts"

    // 复制文件到主机
    sh """
        # 主机连通性检测
        ansible "${targetHosts}" -m ping -i hosts

        # 清理和创建发布目录
        ansible "${targetHosts}" -m shell -a "rm -fr ${targetDir}/${serviceName}/* \\
            && mkdir -p ${targetDir}/${serviceName} || echo file is exists" -i hosts

        # 复制应用文件
        ansible "${targetHosts}" -m copy -a "src=${fileName} dest=${targetDir}/${serviceName}/${fileName}" -i hosts
    """

    // 获取共享库资源文件内容（String字符串） - 发布脚本
    fileData = libraryResource 'scripts/service.sh'
    // 将共享库资源文件内容写入文件
    writeFile file: 'service.sh', text: "${fileData}"
    sh "ls -a ; cat service.sh"

    // 启动应用服务
    sh """
        # 复制脚本
        ansible "${targetHosts}" -m copy -a "src=service.sh dest=${targetDir}/${serviceName}/service.sh"

        # 启动服务
        ansible "${targetHosts}" -m shell -a "cd ${targetDir}/${serviceName} ; source /etc/profile \\
        && sh service.sh ${serviceName} ${version} ${port} start" -u root

        # 检查服务
        sleep 5
        ansible "${targetHosts}" -m shell -a "cd ${targetDir}/${serviceName} ; source /etc/profile \\
        && sh service.sh ${serviceName} ${version} ${port} check" -u root
    """
}

/**
 * 使用 Ansible 脚本部署到目标主机
 *
 * 该函数根据提供的主机列表，利用 Ansible 脚本进行自动化部署。首先会检测目标主机的连通性，
 * 然后远程执行指定脚本进行部署。
 *
 * @param targetHosts 目标主机列表，多个主机用逗号分隔
 * @param script 执行的脚本路径
 * @param version 部署版本
 */
def deployUsingAnsibleScript(targetHosts, script, version) {
    if (targetHosts == null || targetHosts.isEmpty()) {
        echo "Target Hosts is empty."
        return
    }
    if (script == null || script.isEmpty()) {
        echo "Script is empty."
        return
    }
    if (version == null || version.isEmpty()) {
        echo "Version is empty."
        return
    }

    // 输出目标主机列表
    echo "Target Hosts: ${targetHosts}"

    // 删除旧的hosts
    sh "rm -fr hosts"
    // 根据选择的发布主机生成hosts
    targetHosts.split(',').each { host ->  // 使用字符串分隔而不是集合迭代器
        sh "echo ${host} >> hosts"
    }
    sh "cat hosts"

    sh """
        # 主机连通性检测
        ansible "${targetHosts}" -m ping -i hosts

        # 执行部署脚本
        ansible ${targetHosts} -m shell -a "sh ${script} ${version}" -u root -i hosts
    """

    echo "ansible ${targetHosts} -m shell -a sh ${script} ${version} -u root -i hosts"
}
