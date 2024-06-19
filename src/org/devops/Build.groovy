package org.devops

/**
 * Maven构建
 */
def MavenBuild() {
    // 使用绝对路径的执行命令
//    sh "/usr/local/apache-maven/bin/mvn clean package"
    // 配置环境变量的执行命令
    sh "mvn -v && mvn clean install -DskipTests && mvn clean package -DskipTests"
}

/**
 * Maven构建：跳过PMD、CheckStyle、Test、jaxb2检查
 */
def MavenBuildSkipTest() {
    sh "mvn -v && mvn clean install -DskipTests && mvn clean package -Dpmd.skip=true -Dcheckstyle.skip=true -DskipTests -Djaxb2.skip=true"
}

/**
 * 自定义Maven版本构建（基于Linux宿主机环境变量）
 */
def MavenCustomVersionBuild(){
    sh """
       export JAVA_HOME=/opt/jdk1.8.0_391
       mvn -v
       mvn clean package --settings "${M2_HOME}"/conf/settings-jdk-1.8.xml -Dpmd.skip=true -Dcheckstyle.skip=true -DskipTests -Djaxb2.skip=true
    
       # export JAVA_HOME=/opt/jdk-17.0.9
       # export MAVEN_OPTS="-Dmaven.compiler.source=17 -Dmaven.compiler.target=17"
    
       cp -ar /data/nfs/shared-tools/gams/ ./gams
    """
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
 * Jenkins 自定义NodeJS版本构建
 * http://192.168.100.150:8080/manage/configureTools/
 * NodeJS安装：
 *  别名：nodejs-18
 *  版本：NodeJS 18.19.0
 *  Global npm packages to install：npm install -g pnpm
 *  Global npm packages refresh hours：72
 */
def NodeJSCustomVersionBuild(version) {
    nodejs(version) {
        sh """
            node -v && npm -v
            npm cache clean --force
            yarn -v
            yarn config set registry https://registry.npmmirror.com
            yarn && yarn run build
        """
    }
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
        case "nodejs-18":
            NodeJSCustomVersionBuild("nodejs-18")
            break;
        default:
            error "No such tools ... [maven/mavenSkip/gradle/ant/go/npm/yarn]"
            break
    }
}