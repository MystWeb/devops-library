package org.devops

// Maven测试
def MavenTest() {
    sh "mvn test"
    junit 'target/surefire-reports/*.xml'
}

// Gradle测试
def GradleTest() {
    sh "gradle test"
    junit 'build/test-results/test/*.xml'
}

// Ant测试
/*def AntBuild(configPath = "./build.xml") {
    sh "ant -f ${configPath}"
}*/

// Golang测试
def GoTest() {
    sh " go test"
}

// Npm测试
def NpmTest() {
    sh "npm test"
}

// Yarn测试
def YarnTest() {
    sh "yarn test "
}

/**
 * Main-代码测试
 * @param buildTool 构建工具
 */
def CodeTest(buildTool) {
    switch (buildTool) {
        case "maven":
            MavenTest()
            break;
        case "mavenSkip":
            println("maven Skip the unit tests")
            break;
        case "gradle":
            GradleTest()
            break;
        case "go":
            GoTest()
            break;
        case "npm":
            NpmTest()
            break;
        case "npmSkip":
            println("npm Skip the unit tests")
            break;
        case "yarn":
            YarnTest()
            break;
        default:
//            println("No such tools ... [maven/gradle/ant/go/npm/yarn]")
            error "No such tools ... [maven/gradle/ant/go/npm/yarn]"
            break
    }
}