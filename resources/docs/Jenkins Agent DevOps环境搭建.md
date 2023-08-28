# Jenkins Agent DevOps环境搭建

## 一、下载所需工具

> 所有安装包推荐下载至Linux服务器：/opt/install_packages，解压至/opt/目录下

- Git：

```bash
yum install https://packages.endpointdev.com/rhel/7/os/x86_64/endpoint-repo.x86_64.rpm
yum update -y && yum install git && git --version
```

- [JDK](https://www.oracle.com/java/technologies/downloads/)：[jdk-11.0.19_linux-x64_bin.tar.gz](https://www.oracle.com/tw/java/technologies/javase/jdk11-archive-downloads.html#license-lightbox)

```bash
tar -zxvf /opt/install_packages/jdk-11.0.19_linux-x64_bin.tar.gz -C /opt/
```

- [Maven](https://maven.apache.org/)：[apache-maven-3.9.4-bin.tar.gz](https://dlcdn.apache.org/maven/maven-3/3.9.4/binaries/apache-maven-3.9.4-bin.tar.gz)

```bash
tar -zxvf /opt/install_packages/apache-maven-3.9.4-bin.tar.gz -C /opt/
```

- [Gradle](https://gradle.org/install/)：[gradle-8.3-bin.zip](https://gradle.org/next-steps/?version=8.3&format=bin)

```bash
unzip /opt/install_packages/gradle-8.3-bin.zip -d /opt/
```

- [Go](https://go.dev/)：[go1.21.0.linux-amd64.tar.gz](https://go.dev/dl/go1.21.0.linux-amd64.tar.gz)

```bash
tar -zxvf /opt/install_packages/go1.21.0.linux-amd64.tar.gz -C /opt/
```

- [Node.js](https://nodejs.org/zh-cn/download/releases)：[node-v16.20.2-linux-x64.tar.gz](https://nodejs.org/download/release/v16.20.2/node-v16.20.2-linux-x64.tar.gz)

```bash
tar -zxvf /opt/install_packages/node-v16.20.2-linux-x64.tar.gz -C /opt/
```

- [Sonar-Scanner](https://docs.sonarsource.com/sonarqube/latest/analyzing-source-code/scanners/sonarscanner/)：[sonar-scanner-cli-5.0.1.3006-linux.zip](https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-5.0.1.3006-linux.zip)

```bash
unzip /opt/install_packages/sonar-scanner-cli-5.0.1.3006-linux.zip -d /opt/
```

- [Kubectl](https://kubernetes.io/zh-cn/docs/tasks/tools/install-kubectl-linux/)：

> 例如，要在 Linux x86-64 中下载 1.25.5 版本，请输入：curl -LO https://dl.k8s.io/release/v1.25.5/bin/linux/amd64/kubectl

```bash
curl -LO https://dl.k8s.io/release/v1.25.5/bin/linux/amd64/kubectl -o /opt/install_packages/kubectl
sudo install -o root -g root -m 0755 /opt/install_packages/kubectl /usr/local/bin/kubectl
kubectl version --client --output=yaml

# 创建.kube目录并上传KubeConfig文件至/root/.kube下
mkdir ~/.kube && ls -l ~/.kube/config && kubectl version && kubectl get node
```

- [Helm](https://helm.sh/docs/intro/install/)：[helm-v3.12.3-linux-amd64.tar.gz](https://get.helm.sh/helm-v3.12.3-linux-amd64.tar.gz)

```bash
tar -zxvf /opt/install_packages/helm-v3.12.3-linux-amd64.tar.gz -C /opt/
mv /opt/linux-amd64/helm /usr/local/bin/helm && rm -rf /opt/linux-amd64
helm version
```

- ssh key生成并设置公钥 id_ed25519.pub 至GitLab SSH密钥

```bash
mkdir ~/.ssh
cd ~/.ssh/
git config --global user.name "ziming.xing"
git config --global user.email "77784423@qq.com"
ssh-keygen -t ed25519 -C "77784423@qq.com"
```

- Jenkins Master设置私钥文件至凭据

**Jenkins Master | 系统管理 | credentials | 系统 | 全局凭据 | Add Credentials**

> 类型：Secret file
>
> 范围：全局 (Jenkins, nodes, items, all child items, etc)
>
> File：选择id_ed25519私钥文件
>
> ID：留空白自生成
>
> 描述：GitLab-ProAIM-私钥文件（192.168.100.150:/root/.ssh/id_ed25519）

## 二、配置环境变量

```bash
vim /etc/profile
```

```bash
# Tools Environment variable
export JAVA_HOME=/opt/jdk-11.0.19
#export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.17.0.8-2.el7_9.x86_64
export M2_HOME=/opt/apache-maven-3.9.4
export GRADLE_HOME=/opt/gradle-8.3
export GOROOT=/opt/go
export GOPATH=/opt/go-dir
export NODE_HOME=/opt/node-v16.20.2-linux-x64
export SONAR_SCANNER_HOME=/opt/sonar-scanner-5.0.1.3006-linux
export PATH=$SONAR_SCANNER_HOME/bin:$NODE_HOME/bin:$GOROOT/bin:$GRADLE_HOME/bin:$M2_HOME/bin:$JAVA_HOME/bin:$PATH
```

```bash
source /etc/profile
```

## 三、工具版本验证

```bash
[root@localhost ~]# java --version
java 11.0.19 2023-04-18 LTS
Java(TM) SE Runtime Environment 18.9 (build 11.0.19+9-LTS-224)
Java HotSpot(TM) 64-Bit Server VM 18.9 (build 11.0.19+9-LTS-224, mixed mode)
```

```bash
[root@localhost ~]# mvn -v
Apache Maven 3.9.4 (dfbb324ad4a7c8fb0bf182e6d91b0ae20e3d2dd9)
Maven home: /opt/apache-maven-3.9.4
Java version: 11.0.19, vendor: Oracle Corporation, runtime: /opt/jdk-11.0.19
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "3.10.0-1160.92.1.el7.x86_64", arch: "amd64", family: "unix"
```

```bash
[root@localhost ~]# gradle --version

------------------------------------------------------------
Gradle 8.3
------------------------------------------------------------

Build time:   2023-08-17 07:06:47 UTC
Revision:     8afbf24b469158b714b36e84c6f4d4976c86fcd5

Kotlin:       1.9.0
Groovy:       3.0.17
Ant:          Apache Ant(TM) version 1.10.13 compiled on January 4 2023
JVM:          11.0.19 (Oracle Corporation 11.0.19+9-LTS-224)
OS:           Linux 3.10.0-1160.92.1.el7.x86_64 amd64
```

```bash
[root@localhost ~]# go version
go version go1.21.0 linux/amd64
```

```bash
[root@localhost ~]# node -v
v16.20.2

npm install -g yarn
npm config set -g registry https://registry.npmmirror.com/
```

```bash
[root@localhost ~]# sonar-scanner -v
INFO: Scanner configuration file: /opt/sonar-scanner-5.0.1.3006-linux/conf/sonar-scanner.properties
INFO: Project root configuration file: NONE
INFO: SonarScanner 5.0.1.3006
INFO: Java 17.0.7 Eclipse Adoptium (64-bit)
INFO: Linux 3.10.0-1160.92.1.el7.x86_64 amd64
```

## 四、Jenkins Agent安装与配置

### Jenkins Master配置

**Jenkins Master | 系统管理 | 节点管理 | New Node**

> 节点名称：build01
>
> 描述：192.168.100.150（build01）
>
> Number of executors：1
>
> 远程工作目录：/data/jenkins_agent
>
> 标签：build linux k8s docker maven
>
> 用法：尽可能的使用这个节点
>
> 启动方式：通过Java Web启动代理
>
> 可用性：尽量保持代理在线

### Jenkins Agent配置

```bash
# 下载agent.jar 至
curl -sO http://192.168.100.150:8080/jnlpJars/agent.jar -o /data/jenkins_agent/agent.jar
# 生成 secret-file 文件
echo "9fe0cce6b7a3114c49246557f4326de02afdedc7af23776c4c2a8f9c357a9128" > /data/jenkins_agent/secret-file

# 手动启动 Jenkins agent
nohup java -jar /data/jenkins_agent/agent.jar \
    -jnlpUrl http://192.168.100.150:8080/computer/build01/jenkins-agent.jnlp \
    -secret @/data/jenkins_agent/secret-file \
    -workDir "/data/jenkins_agent" \
    > /dev/null 2>&1 &
```

```bash
vim /data/jenkins_agent/jenkins-agent-deploy.sh
```

```shell
#!/bin/bash

# 生成 secret-file 文件
echo "9fe0cce6b7a3114c49246557f4326de02afdedc7af23776c4c2a8f9c357a9128" > /data/jenkins_agent/secret-file

# 延迟启动jenkins agent，预防系统未初始化完毕，环境变量不生效
sleep 10

# 启动 Jenkins agent
nohup java -jar /data/jenkins_agent/agent.jar \
    -jnlpUrl http://192.168.100.150:8080/computer/build02/jenkins-agent.jnlp \
    -secret @/data/jenkins_agent/secret-file \
    -workDir "/data/jenkins_agent" \
    > /dev/null 2>&1 &
```

```bash
vim /data/jenkins_agent/jenkins-agent-stop.sh
```

```shell
#!/bin/bash

pids=$(ps -ef | grep '[j]enkins-agent' | awk '{print $2}')
if [[ -n $pids ]]; then
  echo "Killing jenkins-agent processes: $pids"
  kill $pids
  sleep 5
  kill -9 $pids
fi
```



#### Jenkins Agent配置开机自启动（不推荐）

**注意：开机自启动会丢失系统配置的环境变量**

```bash
vim /etc/systemd/system/jenkins-agent.service
```

```bash
[Unit]
Description=Jenkins Agent Service
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/data/jenkins_agent
ExecStart=/bin/bash -c "sleep 10 && /opt/jdk-11.0.19/bin/java -jar /data/jenkins_agent/agent.jar -jnlpUrl http://192.168.100.150:8080/computer/build01/jenkins-agent.jnlp -secret @/data/jenkins_agent/secret-file -workDir '/data/jenkins_agent'"
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload && systemctl enable --now jenkins-agent.service
```

## 五、命令自动补全

**安装 bash-completion**

```sh
sudo yum install -y bash-completion

安装完成之后重启系统或者重新登录 shell。如果安装成功。键入 docker p 后，再 Tab 键，系统显示如下：
pause   plugin  port    ps      pull    push
```

**Docker Composer 命令自动补全**

```sh
sudo curl -L https://raw.githubusercontent.com/docker/compose/1.27.4/contrib/completion/bash/docker-compose -o /etc/bash_completion.d/docker-compose
source /etc/bash_completion.d/docker-compose
```

**Containerd Ctr 命令自动补全**

```bash
curl -L https://raw.githubusercontent.com/containerd/containerd/main/contrib/autocomplete/ctr -o /etc/bash_completion.d/ctr # ctr自动补全
```

**K8s-Master节点 命令自动补全**

```sh
source /usr/share/bash-completion/bash_completion
source <(kubectl completion bash)
echo "source <(kubectl completion bash)" >> ~/.bashrc
```

**Helm 命令自动补全**

```bash
helm completion bash > .helmrc && echo "source .helmrc" >> .bashrc
```

