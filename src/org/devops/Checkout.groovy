package org.devops

/**
 * 下载代码
 * @param srcUrl 代码地址
 * @param branchName 分支名称
 * @param credentialsId 凭据Id
 * @return
 */
def GetCode(srcUrl, branchName, credentialsId) {
    checkout([
            $class: 'GitSCM',
            branches: [[name: branchName]],
            extensions: [],
            userRemoteConfigs: [[credentialsId: credentialsId, url: srcUrl]]
    ])
}