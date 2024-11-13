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
 * Maven构建：跳过PMD、CheckStyle、Test、jaxb2检查
 */
def MavenBuildSkipTest() {
    sh "mvn -v && mvn clean package -Dpmd.skip=true -Dcheckstyle.skip=true -DskipTests -Djaxb2.skip=true"
}

/**
 * 基于指定的JDK版本执行Maven构建
 * @param jdkVersion JDK版本
 */
def MavenBuildByJDKVersion(jdkVersion) {
    def jdkMap = [
            "1.8": "/opt/jdk1.8.0_391",
            "11" : "/opt/jdk-11.0.19",
            "17" : "/opt/jdk-17.0.9",
            "21" : "/opt/jdk-21.0.5"
    ]

    def jdkHome = jdkMap[jdkVersion]
    if (jdkHome) {
        sh """
            export JAVA_HOME=${jdkHome}
            export MAVEN_OPTS="-Dmaven.compiler.source=${jdkVersion} -Dmaven.compiler.target=${jdkVersion}"
            # mvn clean package --settings "${M2_HOME}"/conf/settings-jdk-1.8.xml -Dpmd.skip=true -Dcheckstyle.skip=true -DskipTests -Djaxb2.skip=true
            mvn -v
            mvn clean package -Dpmd.skip=true -Dcheckstyle.skip=true -DskipTests -Djaxb2.skip=true
        """
    } else {
        error "No such JDK Version ... "
    }
}

/**
 * 基于指定的版本执行Npm构建
 * @param version 版本
 */
def NpmBuildByVersion(version) {
    def map = [
            "16": "/opt/node-v16.20.2-linux-x64",
            "18": "/opt/node-v18.18.2-linux-x64",
    ]

    nodejs(version) {
        sh """
            node -v && npm -v
            npm cache clean --force
            yarn -v
            yarn config set registry https://registry.npmmirror.com
            yarn && yarn run build
        """
    }

    /*def home = map[version]
    if (home) {
        sh """
            export NODE_HOME=${home}
            node -v && npm -v
            npm cache clean --force
            yarn -v
            yarn config set registry https://registry.npmmirror.com
            yarn && yarn run build
        """
    } else {
        error "No such Version ... "
    }*/
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
    sh "node -v && npm -v && npm cache clean --force && npm config set registry https://registry.npmmirror.com && npm install && NODE_OPTIONS=--max-old-space-size=8192 npm run build"
}

/**
 * Yarn构建
 */
def YarnBuild() {
    sh "npm cache clean --force && node -v && npm -v && yarn -v && yarn config set registry https://registry.npmmirror.com && yarn && NODE_OPTIONS=--max-old-space-size=8192 yarn run build"
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
