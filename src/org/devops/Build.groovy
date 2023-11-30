package org.devops

/**
 * Maven构建
 */
def MavenBuild() {
    // 使用绝对路径的执行命令
//    sh "/usr/local/apache-maven/bin/mvn clean package"
    // 配置环境变量的执行命令
    sh "mvn -v && mvn clean package"
}

/**
 * Maven构建：跳过PMD、CheckStyle、Test检查
 */
def MavenBuildSkipTest() {
    sh "mvn -v && mvn clean package -Dpmd.skip=true -Dcheckstyle.skip=true -DskipTests -Djaxb2.skip=true"
}

/**
 * Gradle构建
 */
def GradleBuild() {
    sh "gradle -v && gradle build"
}

/**
 * Ant构建
 */
def AntBuild(configPath = "./build.xml") {
    sh "ant -f ${configPath}"
}

/**
 * Golang构建
 */
def GoBuild(configPath = "demo.go") {
    sh "go version && go build ${configPath}"
}

/**
 * Npm构建
 */
def NpmBuild() {
    sh "node -v && npm -v && npm cache clean --force && npm config set registry https://registry.npmmirror.com && npm install && npm run build"
}

/**
 * Yarn构建
 */
def YarnBuild() {
    sh "npm cache clean --force && node -v && npm -v && yarn -v && yarn config set registry https://registry.npmmirror.com && yarn && yarn run build"
}

/**
 * Main-代码构建
 * @param buildTool 构建工具
 */
def CodeBuild(buildTool) {
    switch (buildTool) {
        case "maven":
            MavenBuild()
            break;
        case "mavenSkip":
            MavenBuildSkipTest()
            break;
        case "gradle":
            GradleBuild()
            break;
        case "ant":
            AntBuild()
            break;
        case "go":
            GoBuild()
            break;
        case "npm":
            NpmBuild()
            break;
        case "yarn":
            YarnBuild()
            break;
        default:
            error "No such tools ... [maven/mavenSkip/gradle/ant/go/npm/yarn]"
            break
    }
}