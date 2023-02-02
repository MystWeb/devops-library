# Docker-Composer安装Harbor私有镜像仓库

## 一、Docker安装

所有节点安装Docker

```sh
必备工具安装：
yum install -y epel-release yum-utils device-mapper-persistent-data lvm2

添加docker源：
sudo yum-config-manager \
    --add-repo \
    http://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo \
    --add-repo \
    https://nvidia.github.io/nvidia-docker/centos7/nvidia-docker.repo
    
所有节点关闭防火墙、selinux、dnsmasq、swap。服务器配置如下：
systemctl disable --now firewalld # 关闭防火墙
systemctl disable --now dnsmasq # 关闭dnsmasq
systemctl disable --now NetworkManager # 关闭NetworkManager

关闭selinux：
cp -p /etc/selinux/config /etc/selinux/config.bak$(date '+%Y%m%d%H%M%S')
sed -i 's#SELINUX=enforcing#SELINUX=disabled#g' /etc/selinux/config # 永久
sed -i 's#SELINUX=enforcing#SELINUX=disabled#g' /etc/sysconfig/selinux # 永久
setenforce 0  # 临时

关闭swap：
cp -p /etc/fstab /etc/fstab.bak$(date '+%Y%m%d%H%M%S')
swapoff -a && sysctl -w vm.swappiness=0 # 临时
sed -ri '/^[^#]*swap/s@^@#@' /etc/fstab # 永久

# 方式一：安装最新版本的Docker Engine和容器（推荐）
sudo yum -y install docker-ce docker-ce-cli containerd.io nvidia-container-toolkit nvidia-docker2 bash-completion

# 方式二：安装特定版本的Docker Engine
	# a.列出并排序您存储库中可用的版本，然后选择并安装。此示例按版本号（从高到低）对结果进行排序
	yum list docker-ce --showduplicates | sort -r
	# b.通过其完全合格的软件包名称安装特定版本，该软件包名称是软件包名称（docker-ce）加上版本字符串（第二列），从第一个冒号（:）一直到第一个连字符，并用连字符（-）分隔。例如，docker-ce-18.09.1
sudo yum install docker-ce-<VERSION_STRING> docker-ce-cli-<VERSION_STRING> containerd.io
# 注意：Docker已安装但尚未启动。docker创建该组，但没有用户添加到该组。
```

### 1.1 Docker配置

> insecure-registries参数：Harbor私有镜像仓库访问地址

```sh
mkdir -p /etc/docker

#cat <<EOF>> /etc/docker/daemon.json
cat > /etc/docker/daemon.json <<EOF
{
    "insecure-registries":["http://192.168.100.150:8082", "https://harbor.devops.com"],
    "exec-opts": ["native.cgroupdriver=systemd"],
    "registry-mirrors": [
        "http://hub-mirror.c.163.com",
        "https://docker.mirrors.ustc.edu.cn"
    ],
    "max-concurrent-downloads": 10,
    "max-concurrent-uploads": 20,
    "default-runtime": "nvidia",
    "runtimes": {
        "nvidia": {
            "path": "nvidia-container-runtime",
            "runtimeArgs": []
        }
    },
    "live-restore": true,
    "log-driver": "json-file",
    "log-opts": {
        "max-size": "500m",
        "max-file": "3"
    }
}
EOF
```

### 1.2 Docker开机自启动

```bash
systemctl daemon-reload && systemctl enable --now docker
```

## 二、Docker-Composer安装

Linux 上我们可以从 Github 上下载它的二进制包来使用，最新发行的版本地址：https://github.com/docker/compose/releases。

运行以下命令以下载 Docker Compose 的当前稳定版本：

```bash
sudo curl -L "https://github.com/docker/compose/releases/download/v2.14.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
```

Docker Compose 存放在 GitHub，不太稳定。

你可以也通过执行下面的命令，高速安装 Docker Compose。

```bash
curl -L https://get.daocloud.io/docker/compose/releases/download/v2.14.2/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose
```

将可执行权限应用于二进制文件并创建软链：

```bash
sudo chmod +x /usr/local/bin/docker-compose
sudo ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
```

测试是否安装成功

```bash
docker-compose version
Docker Compose version v2.14.2
```

## 三、命令自动补全

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

**K8s-Master节点 命令自动补全**

```sh
source /usr/share/bash-completion/bash_completion
source <(kubectl completion bash)
echo "source <(kubectl completion bash)" >> ~/.bashrc
```

## 四、Harbor安装

### 4.1 下载Harbor软件包

可在官方查看对应版本：https://github.com/goharbor/harbor/releases，通过浏览器下载。

也可确认版本后，当前最新版本为v2.7.0，直接下载：

```bash
wget -c https://github.com/goharbor/harbor/releases/download/v2.7.0/harbor-offline-installer-v2.7.0.tgz
```

### 4.2 解压Harbor软件包

将harbor解压到/opt目录下

```bash
[root@devops-app01 opt]# tar -zxvf harbor-offline-installer-v2.7.0.tgz -C /opt && cd /opt/harbor/
```

### 4.3 修改harbor.yml配置文件

```bash
[root@devops-app01 harbor]# cp harbor.yml.tmpl harbor.yml
[root@devops-app01 harbor]# vim harbor.yml
```

```yaml
# Configuration file of Harbor

# The IP address or hostname to access admin UI and registry service.
# DO NOT use localhost or 127.0.0.1, because Harbor needs to be accessed by external clients.
# 修改为harbor server 主机IP或域名
hostname: 192.168.100.150

# http related config
http:
  # port for http, default is 80. If https enabled, this port will redirect to https port
  # 开启http访问，默认为80端口
  port: 8082

# https related config
https:
  # https port for harbor, default is 443
  # 开启https访问，默认为443端口
  port: 443
  # The path of cert and key files for nginx
  # OpenSSL生成自签证书：openssl req -newkey rsa:4096 -nodes -sha256 -keyout /data/harbor/certs/ca.key -x509 -out /data/harbor/certs/ca.crt -subj C=CN/ST=BJ/L=BJ/O=DEVOPS/CN=harbor.devops.cn -days 365000
  # 修改为自己公钥路径及名称
  certificate: /opt/harbor/certs
  private_key: /opt/harbor/certs

# # Uncomment following will enable tls communication between all harbor components
# internal_tls:
#   # set enabled to true means internal tls is enabled
#   enabled: true
#   # put your cert and key files on dir
#   dir: /etc/harbor/tls/internal

# Uncomment external_url if you want to enable external proxy
# And when it enabled the hostname will no longer used
# external_url: https://reg.mydomain.com:8433

# The initial password of Harbor admin
# It only works in first time to install harbor
# Remember Change the admin password from UI after launching Harbor.
# admin账号密码，默认为Harbor12345
harbor_admin_password: devops@2013

# Harbor DB configuration
database:
  # The password for the root user of Harbor DB. Change this before any production use.
  # 数据库密码，默认为root123
  password: devops@2013
  # The maximum number of connections in the idle connection pool. If it <=0, no idle connections are retained.
  max_idle_conns: 100
  # The maximum number of open connections to the database. If it <= 0, then there is no limit on the number of open connections.
  # Note: the default number of connections is 1024 for postgres of harbor.
  max_open_conns: 900
  # The maximum amount of time a connection may be reused. Expired connections may be closed lazily before reuse. If it <= 0, connections are not closed due to a connection's age.
  # The value is a duration string. A duration string is a possibly signed sequence of decimal numbers, each with optional fraction and a unit suffix, such as "300ms", "-1.5h" or "2h45m". Valid time units are "ns", "us" (or "µs"), "ms", "s", "m", "h".
  conn_max_lifetime: 5m
  # The maximum amount of time a connection may be idle. Expired connections may be closed lazily before reuse. If it <= 0, connections are not closed due to a connection's idle time.
  # The value is a duration string. A duration string is a possibly signed sequence of decimal numbers, each with optional fraction and a unit suffix, such as "300ms", "-1.5h" or "2h45m". Valid time units are "ns", "us" (or "µs"), "ms", "s", "m", "h".
  conn_max_idle_time: 0

# The default data volume
# 默认数据卷目录，默认为/data
data_volume: /opt/harbor/data

# Harbor Storage settings by default is using /data dir on local filesystem
# Uncomment storage_service setting If you want to using external storage
# storage_service:
#   # ca_bundle is the path to the custom root ca certificate, which will be injected into the truststore
#   # of registry's and chart repository's containers.  This is usually needed when the user hosts a internal storage with self signed certificate.
#   ca_bundle:

#   # storage backend, default is filesystem, options include filesystem, azure, gcs, s3, swift and oss
#   # for more info about this configuration please refer https://docs.docker.com/registry/configuration/
#   filesystem:
#     maxthreads: 100
#   # set disable to true when you want to disable registry redirect
#   redirect:
#     disabled: false

# Trivy configuration
#
# Trivy DB contains vulnerability information from NVD, Red Hat, and many other upstream vulnerability databases.
# It is downloaded by Trivy from the GitHub release page https://github.com/aquasecurity/trivy-db/releases and cached
# in the local file system. In addition, the database contains the update timestamp so Trivy can detect whether it
# should download a newer version from the Internet or use the cached one. Currently, the database is updated every
# 12 hours and published as a new release to GitHub.
trivy:
  # ignoreUnfixed The flag to display only fixed vulnerabilities
  ignore_unfixed: false
  # skipUpdate The flag to enable or disable Trivy DB downloads from GitHub
  #
  # You might want to enable this flag in test or CI/CD environments to avoid GitHub rate limiting issues.
  # If the flag is enabled you have to download the `trivy-offline.tar.gz` archive manually, extract `trivy.db` and
  # `metadata.json` files and mount them in the `/home/scanner/.cache/trivy/db` path.
  skip_update: false
  #
  # The offline_scan option prevents Trivy from sending API requests to identify dependencies.
  # Scanning JAR files and pom.xml may require Internet access for better detection, but this option tries to avoid it.
  # For example, the offline mode will not try to resolve transitive dependencies in pom.xml when the dependency doesn't
  # exist in the local repositories. It means a number of detected vulnerabilities might be fewer in offline mode.
  # It would work if all the dependencies are in local.
  # This option doesn’t affect DB download. You need to specify "skip-update" as well as "offline-scan" in an air-gapped environment.
  offline_scan: false
  #
  # Comma-separated list of what security issues to detect. Possible values are `vuln`, `config` and `secret`. Defaults to `vuln`.
  security_check: vuln
  #
  # insecure The flag to skip verifying registry certificate
  insecure: false
  # github_token The GitHub access token to download Trivy DB
  #
  # Anonymous downloads from GitHub are subject to the limit of 60 requests per hour. Normally such rate limit is enough
  # for production operations. If, for any reason, it's not enough, you could increase the rate limit to 5000
  # requests per hour by specifying the GitHub access token. For more details on GitHub rate limiting please consult
  # https://developer.github.com/v3/#rate-limiting
  #
  # You can create a GitHub token by following the instructions in
  # https://help.github.com/en/github/authenticating-to-github/creating-a-personal-access-token-for-the-command-line
  #
  # github_token: xxx

jobservice:
  # Maximum number of job workers in job service
  max_job_workers: 10

notification:
  # Maximum retry count for webhook job
  webhook_job_max_retry: 10

chart:
  # Change the value of absolute_url to enabled can enable absolute url in chart
  absolute_url: disabled

# Log configurations
log:
  # options are debug, info, warning, error, fatal
  level: info
  # configs for logs in local storage
  local:
    # Log files are rotated log_rotate_count times before being removed. If count is 0, old versions are removed rather than rotated.
    rotate_count: 50
    # Log files are rotated only if they grow bigger than log_rotate_size bytes. If size is followed by k, the size is assumed to be in kilobytes.
    # If the M is used, the size is in megabytes, and if G is used, the size is in gigabytes. So size 100, size 100k, size 100M and size 100G
    # are all valid.
    rotate_size: 200M
    # The directory on your host that store log
    location: /var/log/harbor

  # Uncomment following lines to enable external syslog endpoint.
  # external_endpoint:
  #   # protocol used to transmit log to external endpoint, options is tcp or udp
  #   protocol: tcp
  #   # The host of external endpoint
  #   host: localhost
  #   # Port of external endpoint
  #   port: 5140

#This attribute is for migrator to detect the version of the .cfg file, DO NOT MODIFY!
_version: 2.7.0

# Uncomment external_database if using external database.
# external_database:
#   harbor:
#     host: harbor_db_host
#     port: harbor_db_port
#     db_name: harbor_db_name
#     username: harbor_db_username
#     password: harbor_db_password
#     ssl_mode: disable
#     max_idle_conns: 2
#     max_open_conns: 0
#   notary_signer:
#     host: notary_signer_db_host
#     port: notary_signer_db_port
#     db_name: notary_signer_db_name
#     username: notary_signer_db_username
#     password: notary_signer_db_password
#     ssl_mode: disable
#   notary_server:
#     host: notary_server_db_host
#     port: notary_server_db_port
#     db_name: notary_server_db_name
#     username: notary_server_db_username
#     password: notary_server_db_password
#     ssl_mode: disable

# Uncomment external_redis if using external Redis server
# external_redis:
#   # support redis, redis+sentinel
#   # host for redis: <host_redis>:<port_redis>
#   # host for redis+sentinel:
#   #  <host_sentinel1>:<port_sentinel1>,<host_sentinel2>:<port_sentinel2>,<host_sentinel3>:<port_sentinel3>
#   host: redis:6379
#   password: 
#   # sentinel_master_set must be set to support redis+sentinel
#   #sentinel_master_set:
#   # db_index 0 is for core, it's unchangeable
#   registry_db_index: 1
#   jobservice_db_index: 2
#   chartmuseum_db_index: 3
#   trivy_db_index: 5
#   idle_timeout_seconds: 30

# Uncomment uaa for trusting the certificate of uaa instance that is hosted via self-signed cert.
# uaa:
#   ca_file: /path/to/ca

# Global proxy
# Config http proxy for components, e.g. http://my.proxy.com:3128
# Components doesn't need to connect to each others via http proxy.
# Remove component from `components` array if want disable proxy
# for it. If you want use proxy for replication, MUST enable proxy
# for core and jobservice, and set `http_proxy` and `https_proxy`.
# Add domain to the `no_proxy` field, when you want disable proxy
# for some special registry.
proxy:
  http_proxy:
  https_proxy:
  no_proxy:
  components:
    - core
    - jobservice
    - trivy

# metric:
#   enabled: false
#   port: 9090
#   path: /metrics

# Trace related config
# only can enable one trace provider(jaeger or otel) at the same time,
# and when using jaeger as provider, can only enable it with agent mode or collector mode.
# if using jaeger collector mode, uncomment endpoint and uncomment username, password if needed
# if using jaeger agetn mode uncomment agent_host and agent_port
# trace:
#   enabled: true
#   # set sample_rate to 1 if you wanna sampling 100% of trace data; set 0.5 if you wanna sampling 50% of trace data, and so forth
#   sample_rate: 1
#   # # namespace used to differenciate different harbor services
#   # namespace:
#   # # attributes is a key value dict contains user defined attributes used to initialize trace provider
#   # attributes:
#   #   application: harbor
#   # # jaeger should be 1.26 or newer.
#   # jaeger:
#   #   endpoint: http://hostname:14268/api/traces
#   #   username:
#   #   password:
#   #   agent_host: hostname
#   #   # export trace data by jaeger.thrift in compact mode
#   #   agent_port: 6831
#   # otel:
#   #   endpoint: hostname:4318
#   #   url_path: /v1/traces
#   #   compression: false
#   #   insecure: true
#   #   timeout: 10s

# enable purge _upload directories
upload_purging:
  enabled: true
  # remove files in _upload directories which exist for a period of time, default is one week.
  age: 168h
  # the interval of the purge operations
  interval: 24h
  dryrun: false

# cache layer configurations
# If this feature enabled, harbor will cache the resource
# `project/project_metadata/repository/artifact/manifest` in the redis
# which can especially help to improve the performance of high concurrent
# manifest pulling.
# NOTICE
# If you are deploying Harbor in HA mode, make sure that all the harbor
# instances have the same behaviour, all with caching enabled or disabled,
# otherwise it can lead to potential data inconsistency.
cache:
  # not enabled by default
  enabled: false
  # keep cache for one day by default
  expire_hours: 24

```

参数说明：

- hostname：配置主机名称，不可以设置127.0.0.1，localhost这样的主机名，可以是IP或者域名
- ui_url_protocol：指定使用HTTP协议还是HTTPS协议
- Email settings：邮箱设置，option配置，只在首次启动生效，可以登陆UI后修改
- harbor_admin_password：设置管理员的初始密码，只在第一次登录时- 使用
- auth_mode：用户认证模式，默认是db_auth,也可以使用ldap_auth验证。
- db_password：使用db需要指定连接数据库的密码
- self_registration：是否允许自行注册用户，默认是on,新版本可以在图形界面中修改。
- max_job_workers：最大工作数，默认是10个
- customize_crt：是否为token生成证书，默认为on
- ssl_cert：nginx cert与key文件的路径, 只有采用https协议是才有意义
- ssl_cert：nginx cert与key文件的路径, 只有采用https协议是才有意义
- secretkey_path：The path of secretkey storage
- admiral_url：Admiral's url, comment this attribute, or set its value to NA when harbor is standalone
- clair_db_password：未启用calir服务，但解压目录下的"./prepare"文件中要检查以下相关参数配置，不能注释，否则环境准备检查不能通过，报"ConfigParser.NoOptionError: No option u'clair_db_password' in section: u'configuration' "相关错误；或者在"./prepare"中注释相关检查与定义，但需要注意，文件中的关联太多，推荐修改"harbor.cfg"文件即可
- ldap_url：ladp相关设置，如未采用ldap认证，但解压目录下的"./prepare"文件中要检查以下相关参数配置，不能注释，否则环境准备检查不能通过，报"ConfigParser.NoOptionError: No option u'ldap_timeout' in section: u'configuration' "相关错误；或者在"./prepare"中注释相关检查与定义，但需要注意，文件中的关联太多，推荐修改"harbor.cfg"文件即可
- token_expiration：token有效时间，默认30minutes
- project_creation_restriction：创建项目权限控制，默认是"everyone"(所有人)，可设置为"adminonly"(管理员)
- verify_remote_cert：与远程registry通信时是否采用验证ssl

更多详细参数参考官方链接：https://goharbor.io/docs/2.7.0/install-config/configure-yml-file/

### 4.4 创建对应目录（根据配置文件创建）

```bash
mkdir -p /opt/harbor/certs /opt/harbor/data && cd /opt/harbor/certs
```

### 4.5 OpenSSL生成自签证书

```bash
openssl req -newkey rsa:4096 -nodes -sha256 -keyout /opt/harbor/certs/ca.key -x509 -out /opt/harbor/certs/ca.crt -subj C=CN/ST=BJ/L=BJ/O=DEVOPS/CN=harbor.devops.cn -days 36500
```

- req 产生证书签发申请命令
- -newkey 生成新私钥
- rsa:4096 生成秘钥位数
- -nodes 表示私钥不加密
- -sha256 使用SHA-2哈希算法
- -keyout 将新创建的私钥写入的文件名
- -x509 签发X.509格式证书命令。X.509是最通用的一种签名证书格式。
- -out 指定要写入的输出文件名
- -subj 指定用户信息
- -days 有效期（36500表示100年）

查看证书

```bash
ls -lrt /opt/harbor/certs/
```

### 4.6 Harbor安装

```bash
[root@devops-app01 certs]# cd /opt/harbor
[root@devops-app01 harbor]# ./install.sh
```

### 4.7 访问harbor页面测试

通过浏览器访问harbor主页http://192.168.100.150:8082，页面会自动跳转到https。

如果配置了主机名解析或DNS，可以通过域名来访问，如：https://harbor.devops.cn

## 五、harbor服务基本操作

### 5.1 停止harbor

```bash
docker-compose stop
```

### 5.2 启动harbor

```bash
docker-compose start
```

### 5.3 重新配置harbor

要重新配置，请执行以下步骤：

```bash
# 停止harbor
[root@devops-app01 harbor]# docker-compose down -v
# 更新harbor.yml
[root@devops-app01 harbor]# vim harbor.yml
# 运行prepare脚本以填充配置
[root@devops-app01 harbor]# ./prepare
# 重新创建并启动harbor 实例
[root@devops-app01 harbor]# docker-compose up -d
```

## 六、docker上传镜像到私有仓库

可以通过其它docker主机[推荐]，来登录并上传镜像到仓库，也可用harbor主机进行测试验证。

### 6.1 添加镜像信任仓库

修改docker配置文件，添加本机信任可以访问镜像仓库一行："insecure-registries": ["https://harbor.devops.cn"]，如果有多行参数，上一行末尾添加逗号。

```bash
vi /etc/docker/daemon.json
```

```bash
{
    "insecure-registries":["http://192.168.100.150:8082", "https://harbor.devops.cn"],
    "registry-mirrors": [
        "http://hub-mirror.c.163.com",
        "https://docker.mirrors.ustc.edu.cn"
    ]
}
```

### 6.2 重启加载配置并重启docker服务

> 如果daemon.json配置语法有问题，docker服务会启动失败。

```bash
systemctl daemon-reload && systemctl restart docker
```

### 6.3 登入私有仓库

配置host配置文件

```bash
vi /etc/hosts
......
192.168.100.150 harbor.devops.cn
```

登入地址

详细登入方式：docker login -u ${USERNAME} -p ${PASSWORD} ${harbor_Server_IP}:${port}，如：

```bash
# 登陆方式一
docker login -u admin -p devops@2013 harbor.devops.cn
# 登录方式二（推荐）
echo "devops@2013" | docker login -u admin --password-stdin harbor.devops.cn
# 登录方式三（所有方式均支持HTTPS协议登录）
docker login https://harbor.devops.cn
```

### 6.4 常见问题

如果报类似错误：

```bash
Error response from daemon: Get "https://harbor.devops.cn/v2/": x509: certificate relies on legacy Common Name field, use SANs or temporarily enable Common Name matching with GODEBUG=x509ignoreCN=0
```

停止harbor

```bash
docker-compose stop
```

修改配置在/etc/docker/daemon.json中添加可访问的远程registry，详见 添加镜像信任仓库 章节，修改后启动harbor

```bash
docker-compose start
```

## 七、上传本地镜像到harbor仓库

拉取远端的nginx到本地

```bash
docker pull nginx:1.23.3
```

将本地镜像打上tag

```bash
docker tag nginx:1.23.3 harbor.devops.cn/devops/nginx:1.23.3
docker image ls | grep "nginx"
```

上传镜像

```bash
docker push harbor.devops.cn/devops/nginx:1.23.3
```

下载镜像

```bash
docker pull harbor.devops.cn/devops/nginx:1.23.3
```

## 八、Containerd配置私有镜像仓库

> containerd 实现了 kubernetes 的 Container Runtime Interface (CRI) 接口，提供容器运行时核心功能，如镜像管理、容器管理等，相比 dockerd 更加简单、健壮和可移植。
>
> 从docker过度还是需要一点时间慢慢习惯的，今天来探讨containerd 如何从无域名与权威证书的私有仓库harbor，下载镜像！
>
> containerd 不能像docker一样 `docker login harbor.example.com` 登录到镜像仓库,无法从harbor拉取到镜像。

修改Containerd配置文件

```bash
vim /etc/containerd/config.toml
```

- [plugins."io.containerd.grpc.v1.cri".registry.mirrors."docker.io"]：镜像仓库源地址
- endpoint = ["https://registry-1.docker.io"]：镜像仓库代理地址
- insecure_skip_verify = true：是否跳过安全认证
- [plugins."io.containerd.grpc.v1.cri".registry.configs."192.168.100.150:8082".auth]：私有镜像仓库授权认证
  - 配置私有镜像仓库账号密码后，k8s Pod拉取镜像无需创建Secrets，Deployment也无需配置Secrets

- 配置文件参考：https://github.com/containerd/containerd/blob/main/docs/cri/registry.md

```toml
    [plugins."io.containerd.grpc.v1.cri".registry]
      config_path = ""

      [plugins."io.containerd.grpc.v1.cri".registry.auths]

      [plugins."io.containerd.grpc.v1.cri".registry.configs]
        [plugins."io.containerd.grpc.v1.cri".registry.configs."192.168.100.150:8082".tls]
          insecure_skip_verify = true  # 是否跳过安全认证
        [plugins."io.containerd.grpc.v1.cri".registry.configs."192.168.100.150:8082".auth]
          username = "admin"
          password = "devops@2013"
      [plugins."io.containerd.grpc.v1.cri".registry.headers]

      [plugins."io.containerd.grpc.v1.cri".registry.mirrors]
        [plugins."io.containerd.grpc.v1.cri".registry.mirrors."docker.io"]
          endpoint = ["https://registry-1.docker.io"]
        [plugins."io.containerd.grpc.v1.cri".registry.mirrors."192.168.100.150:8082"]
          endpoint = ["http://192.168.100.150:8082"]
```

拉取和查看镜像

```bash
ctr -n k8s.io image pull 192.168.100.150:8082/devops/devops-demo-service:RELEASE-1.2.0-fc67c4d5 --plain-http --user admin:Harbor12345
ctr -n k8s.io image ls
```
