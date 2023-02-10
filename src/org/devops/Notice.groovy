package org.devops

/**
 * 电子邮件通知
 * @param address 邮箱地址（收件人）
 * @param buildStatus 构建状态
 * @return
 */
def EmailNotice(emailAddress, buildStatus) {
    emailext body: """
            <!DOCTYPE html> 
            <html> 
            <head> 
            <meta charset="UTF-8"> 
            </head> 
            <body leftmargin="8" marginwidth="0" topmargin="8" marginheight="4" offset="0"> 
                <img src="https://www.jenkins.io/images/logos/plumber/256.png" width="187" height="256">
                <table width="95%" cellpadding="0" cellspacing="0" style="font-size: 11pt; font-family: Tahoma, Arial, Helvetica, sans-serif">   
                    <tr> 
                        <td><br /> 
                            <b><font color="#0B610B">构建信息</font></b> 
                        </td> 
                    </tr> 
                    <tr> 
                        <td> 
                            <ul> 
                                <li>项目名称：${JOB_NAME}</li>         
                                <li>构建编号：${BUILD_ID}</li> 
                                <li>构建状态: ${buildStatus} </li>                         
                                <li>项目地址：<a href="${BUILD_URL}">${BUILD_URL}</a></li>    
                                <li>构建日志：<a href="${BUILD_URL}console">${BUILD_URL}console</a></li> 
                            </ul> 
                        </td> 
                    </tr> 
                    <tr>  
                </table> 
            </body> 
            </html>  """,
            subject: "Jenkins-${JOB_NAME}项目构建信息 ",
            to: emailAddress
}

/**
 * 钉钉通知-Api
 * @param dingTalkTokenCredentialsId 钉钉群机器人的 access_token 凭据ID
 * 钉钉群机器人的 access_token 可以通过钉钉群管理后台的「群机器人」功能获取，生成方式如下：
 登录钉钉管理后台，打开左侧「应用」菜单。
 点击「创建群机器人」，填写完基本信息后，点击「创建」。
 创建完成后，点击「复制」，即可获取 access_token。
 将 access_token 提供给需要使用的系统或应用。
 注意：钉钉群机器人的 access_token 仅供内部使用，请勿泄露。
 */
def dingTalkNotice(dingTalkTokenCredentialsId) {
    withCredentials([string(credentialsId: "${dingTalkTokenCredentialsId}", variable: 'DINGTALK_ROBOT_TOKEN')]) {
        def ddApi = "https://oapi.dingtalk.com/robot/send?access_token=${DINGTALK_ROBOT_TOKEN}"
        def ddMessage = """
        {
            "msgtype": "text",
            "text": {
                "content": "Jenkins Job '${env.JOB_NAME} [${currentBuild.displayName}]' Build finished, see details: ${env.BUILD_URL}"
            }
        }
    """
        sh "curl -H 'Content-Type: application/json' -X POST -d '${ddMessage}' ${ddApi}"
    }
}

/**
 * 钉钉通知-Plugin
 * 插件链接：https://plugins.jenkins.io/dingding-notifications
 * 参考链接：https://www.jianshu.com/p/563db03e1ed9
 */
def dingTalkPluginNotice(dingTalkRobotId) {
    // Jenkins DingTalk插件的钉钉通知代码
    withCredentials([string(credentialsId: "${dingTalkRobotId}", variable: 'DINGTALK_ROBOT_ID')]) {
        dingtalk robot: "${DINGTALK_ROBOT_ID}",
                type: "MARKDOWN",
                title: """ ["${env.JOB_NAME}"]("${env.BUILD_URL}") """,
                when: ['SUCCESS', 'FAILURE', 'UNSTABLE'],
                picUrl: 'https://www.jenkins.io/images/logos/cute/cute.png',
                text: ["""
                ## Jenkins 钉钉通知
                - 任务：["${currentBuild.displayName}"]("${env.BUILD_URL}")
                - 状态：<font color="${currentBuild.currentResult}" == 'SUCCESS' ? 'green' : "${currentBuild.currentResult}" == 'FAILURE' ? 'red' : 'yellow' >"${currentBuild.currentResult}"</font>
                - 持续时间："${currentBuild.durationString}"
                - 执行人："${env.BUILD_USER}"
                """],
                atAll: false
    }
}