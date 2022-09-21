package org.devops

/**
 * Ansible 多主机远程发布
 * 1、Jenkins Agent节点、主机节点列表都需安装ansible：yum -y install epel-release ansible
 * 2、Jenkins Agent节点与主机节点列表实现免密登录：ssh-keygen -t rsa && ssh-copy-id root@localhost
 * 3、主机节点列表安装运行环境：yum -y install java-11-openjdk java-11-openjdk-devel
 * @param deployHosts 远程发布主机列表
 * @param targetDir 远程发布主机目录
 * @param serviceName 服务/项目名称
 * @param releaseVersion 服务版本号（推荐定义："${branchName}-${commitId}"）
 * @param fileName 文件名称
 * @param port 服务监听的端口号
 */
def AnsibleDeploy(deployHosts, targetDir, serviceName, releaseVersion, fileName, port) {
    // 删除旧的hosts
    sh "rm -fr hosts"
    // 根据选择的发布主机生成hosts
    for (final def host in "${deployHosts}".split(',')) {
        sh "echo ${host} >> hosts"
    }
    sh "cat hosts"

    // Ansible 复制文件到主机
    sh """
        # 主机连通性检测
        ansible "${deployHosts}" -m ping -i hosts
        
        # 清理和创建发布目录
        ansible "${deployHosts}" -m shell -a "rm -fr ${targetDir}/${serviceName}/* \\ 
            && mkdir -p ${targetDir}/${serviceName} || echo file is exists"

        # 复制应用文件
        ansible "${deployHosts}" -m copy -a "src=${fileName} dest=${targetDir}/${serviceName}/${fileName}"
    """

    // 获取共享库资源文件内容（String字符串） - 发布脚本
    fileData = libraryResource 'scripts/service.sh'
    // 将共享库资源文件内容写入文件
    writeFile file: 'service.sh', text: "${fileData}"
    sh "ls -a ; cat service.sh"

    // Ansible 启动应用服务
    sh """
        # 复制脚本
        ansible "${deployHosts}" -m copy -a "src=service.sh dest=${targetDir}/${serviceName}/service.sh"


        # 启动服务
        ansible "${deployHosts}" -m shell -a "cd ${targetDir}/${serviceName} ; source /etc/profile \\ 
&& sh service.sh ${serviceName} ${releaseVersion} ${port} start" -u root

        # 检查服务
        ansible "${deployHosts}" -m shell -a "cd ${targetDir}/${serviceName} ; source /etc/profile \\\\ 
&& sh service.sh ${serviceName} ${releaseVersion} ${port} check" -u root
    """
}
