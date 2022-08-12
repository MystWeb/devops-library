package org.devops


/**
 * Main-自定义命令
 * @param customCommand 自定义命令
 */
def CustomCommands(customCommand) {
    if ("" == customCommand || customCommand.trim().length() <= 0) {
        error "The parameter cannot be null！"
    }

    // 按空格分割自定义构建命令
    def customSplit = customCommand.split(' ')
    // 敏感命令List
    def validCustomCommands = ['rm', 'cat', 'uname', 'rpm', 'yum',
                               'ps', 'ls', 'grep', 'find', 'systemctl',
                               'lsof', 'netstat', 'ssh', 'sudo', 'echo',
                               'df', 'top', 'dpkg', 'crontab', 'iptables',
                               'last', 'route', 'arp', 'nc', 'awk',
                               'mount']

    // 校验自定义命令是否包含敏感命令
    if (validCustomCommands.any { customSplit.contains(it) }) {
        error "Build scripts have security risks. Check your build scripts！"
    } else {
        sh customCommand
    }

}