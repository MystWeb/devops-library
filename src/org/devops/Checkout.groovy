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
            extensions: [
                    [
                            $class: 'SubmoduleOption',
                            disableSubmodules: false,
                            parentCredentials: false,
                            recursiveSubmodules: true, // 如果需要递归检出子模块的源码内容，请设置为 true
                            reference: '', // 可选，可以使用的提交哈希或标签进行子模块检出
                            trackingSubmodules: false // 如果需要跟踪子模块的提交，请设置为 true
                    ]
            ],
            submoduleCfg: [],
            userRemoteConfigs: [[credentialsId: credentialsId, url: srcUrl]]
    ])
}