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
        def messageContent = "Jenkins Job '${env.JOB_NAME} [${currentBuild.displayName}]' Build finished, see details: ${env.BUILD_URL}"
        def ddMessage = """
        {
            "msgtype": "text",
            "text": {
                "content": "${messageContent}"
            }
        }
        """
        // 发送钉钉通知并捕获可能的错误
        try {
            sh "curl -H 'Content-Type: application/json' -X POST -d '${ddMessage}' ${ddApi}"
        } catch (e) {
            echo "Failed to send DingTalk notification: ${e.message}"
        }

    }
}

/**
 * 钉钉通知-Plugin
 * 插件链接：https://plugins.jenkins.io/dingding-notifications
 * 参考链接：https://www.jianshu.com/p/563db03e1ed9、
 * https://open.dingtalk.com/document/orgapp/faq-robot#afd23bb55em7r
 */
def dingTalkPluginNotice(dingTalkRobotId) {
    // Jenkins DingTalk插件的钉钉通知代码
    withCredentials([string(credentialsId: "${dingTalkRobotId}", variable: 'DINGTALK_ROBOT_ID')]) {
        def color = "${currentBuild.currentResult}" == 'SUCCESS' ? 'green' : "${currentBuild.currentResult}" == 'FAILURE' ? 'red' : 'orange'
        def newDescription = "${currentBuild.description}".replaceAll("\n", " \n &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
        println("description：${currentBuild.description} \n newDescription：${newDescription}")
        dingtalk robot: "${DINGTALK_ROBOT_ID}",
                type: "MARKDOWN",
                // 首屏会话 透出的展示内容
                title: "Jenkins 钉钉通知 - [${env.JOB_NAME}](${env.JOB_URL})",
                // 图片 URL（LINK 类型的消息）
                picUrl: 'https://www.jenkins.io/images/logos/cute/cute.png',
                text: [
                        "### Jenkins JOB：[${env.JOB_NAME}](${env.JOB_URL})",
                        "---",
                        "- 任务：[${currentBuild.displayName}](${env.BUILD_URL})",
                        "- 状态：<font color=${color}>${currentBuild.currentResult}</font>",
                        "- 持续时间：${currentBuild.durationString}".split("and counting")[0],
                        "- 执行人：${currentBuild.buildCauses.shortDescription}",
                        "- 描述：${newDescription}"
                ],
                atAll: false
    }
}

/**
 * 钉钉通知-Plugin（支持自定义Markdown描述与富文本格式）
 * @param dingTalkRobotId 钉钉机器人凭据ID
 * @param title 自定义标题（可选）
 * @param customContent 自定义内容（支持List<String>或String类型）
 * @param mentionAll 是否@所有人（默认false）
 */
def dingTalkPluginNotice(String dingTalkRobotId, String title = null, def customContent, boolean mentionAll = false) {
    withCredentials([string(credentialsId: dingTalkRobotId, variable: 'DINGTALK_ROBOT_ID')]) {
        // 处理标题（默认使用JOB名称）
        def finalTitle = title ?: "Jenkins 通知 - [${env.JOB_NAME}](${env.JOB_URL})"

        // 处理状态颜色（若构建结果为空，默认为灰色）
        def color = currentBuild.currentResult ?: 'gray'
        color = color == 'SUCCESS' ? 'green' :
                color == 'FAILURE' ? 'red' :
                        color == 'UNSTABLE' ? 'orange' : 'gray'

        // 构建基础内容块
        def baseContent = [
                "### Jenkins JOB：[${env.JOB_NAME}](${env.JOB_URL})",
                "---",
                "- 任务：[${currentBuild.displayName ?: '无'}](${env.BUILD_URL})",
                "- 状态：<font color=${color}>${currentBuild.currentResult ?: '未执行'}</font>",
                "- 持续时间：${currentBuild.durationString?.split('and counting')[0] ?: 'N/A'}",
                "- 执行人：${currentBuild.buildCauses?.shortDescription ?: '系统触发'}"
        ]

        // 处理自定义内容（支持String或List<String>）
        def contentList = []
        if (customContent instanceof String) {
            contentList = customContent.readLines()
        } else if (customContent instanceof List) {
            contentList = customContent
        } else if (customContent) {
            contentList = [customContent.toString()]
        }

        // 添加描述内容（优先使用自定义内容，其次使用构建描述）
        def descriptionContent = contentList.isEmpty() ?
                currentBuild.description.readLines().collect { "  - ${it}" } :
                contentList.collect { it.startsWith('-') ? it : "  - ${it}" }

        // 合并最终内容
        def finalText = baseContent + ["- 描述："] + descriptionContent

        // 发送钉钉通知
        dingtalk robot: DINGTALK_ROBOT_ID,
                type: "MARKDOWN",
                title: finalTitle,
                picUrl: 'https://www.jenkins.io/images/logos/cute/cute.png',
                text: finalText,
                atAll: mentionAll
    }
}