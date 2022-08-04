package org.devops

/**
 * 代码扫描-Sonar
 * @param projectVersion 代码扫描-Sonar-项目版本
 */
def CodeScan_Sonar(projectVersion) {
    cliPath = "/data/cicd/sonar-scanner/bin"
    withCredentials([usernamePassword(credentialsId: '05d7379e-28a6-4dd2-9b35-1f907a1a05c8',
            usernameVariable: 'SONAR_USERNAME',
            passwordVariable: 'SONAR_PASSWORD')]) {
        // 远程构建时推荐使用CommitID作为代码扫描-项目版本
        sh """
            ${cliPath}/sonar-scanner \
            -Dsonar.login=${SONAR_USERNAME} \
            -Dsonar.password=${SONAR_PASSWORD} \
            -Dsonar.projectVersion=${projectVersion}
        """
    }
}