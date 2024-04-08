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
                            recursiveSubmodules: true, // 设置为true以递归检出子模块
                            trackingSubmodules: false // 设置为true以跟踪子模块提交
                    ],
                    // 用于指定本地分支的名称
                    [
                            $class: 'LocalBranch',
                            localBranch: branchName // 指定本地分支名称，与远程分支同名
                    ],
            ],
//            submoduleCfg: [],
            userRemoteConfigs: [[credentialsId: credentialsId, url: srcUrl]]
    ])
}