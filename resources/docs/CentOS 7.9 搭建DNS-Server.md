# CentOS 7.9 搭建DNS-Server

bind下载地址：https://www.isc.org/bind/

bind安装文档：https://copr.fedorainfracloud.org/coprs/isc/bind/

## 一、安装DNS相关工具—bind

- 添加isc-bind官方源

```bash
yum-config-manager --add-repo https://copr.fedorainfracloud.org/coprs/isc/bind/repo/epel-7/isc-bind-epel-7.repo
```

- 修改DNS主服务器网卡的DNS配置

```bash
vim /etc/sysconfig/network-scripts/ifcfg-eth0
```

```bash
TYPE="Ethernet"
PROXY_METHOD="none"
BROWSER_ONLY="no"
BOOTPROTO="static" # 设置为静态IP 默认dhcp -修改
DEFROUTE="yes"
IPV4_FAILURE_FATAL="no"
IPV6INIT="yes"
IPV6_AUTOCONF="yes"
IPV6_DEFROUTE="yes"
IPV6_FAILURE_FATAL="no"
IPV6_ADDR_GEN_MODE="stable-privacy"
NAME="eth0"
# Version 1 UUID生成：https://www.uuidgenerator.net
UUID="20ee6844-24c3-4e8a-9ed4-f4f2c379ca50" # UUID -修改
DEVICE="eth0"
ONBOOT="yes"
IPADDR="192.168.100.101" # IP地址 -增加
NETMASK="255.255.255.0" # 子网掩码 -增加
GATEWAY="192.168.100.1" # 网关 -增加
DNS1="127.0.0.1" # DNS服务器主机只需指定自己
#DNS2="114.114.114.114" # DNS服务器1 -增加（如需局域网服务器，注释该选项）
```

- 安装bind、bind-utils软件

```bash
yum -y install isc-bind-bind isc-bind-bind-utils
```

- 配置bind工具集环境变量

```bash
vim ~/.bash_profile
```

> 末尾添加isc-bind相关工具集

```bash
source scl_source enable isc-bind
```

- 查看版本号

```bash
[root@localhost ~]# named -version
BIND 9.18.18 (Extended Support Version) <id:1f8de2f>
```

## 二、实现主DNS服务器

```bash
cp named.conf named.conf.bak$(date '+%Y%m%d%H%M%S')
vim /etc/opt/isc/isc-bind/named.conf
```

```c++
options {
        directory "/var/opt/isc/isc-bind/named/data";
        // 设置listen为主机所有IP
        listen-on { localhost; };
        listen-on-v6 { ::1; };
        dnssec-validation auto;
};

logging {
        channel default_debug {
                file "named.run";
                print-time yes;
                severity dynamic;
        };
};
```

```bash
systemctl enable --now isc-bind-named
```

## 三、实现正向DNS主服务器

- 各种资源记录

  - 区域解析库：由众多资源记录RR(Resource Record)组成 记录类型：A, AAAA, PTR, SOA, NS, CNAME, MX SOA：Start Of Authority，起始授权记录；一个区域解析库有且仅能有一个SOA记录，必须位于解 析库的第一条记录

  - A：internet Address，作用，FQDN --> IP AAAA：FQDN --> IPv6 PTR：PoinTeR，IP --> FQDN NS：Name Server，专用于标明当前区域的DNS服务器

  - CNAME ： Canonical Name，别名记录

  - MX：Mail eXchanger，邮件交换器

  - TXT：对域名进行标识和说明的一种方式，一般做验证记录时会使用此项，如：SPF（反垃圾邮 件）记录，https验证等，如下示例：

```assembly
_dnsauth TXT 2012011200000051qgs69bwoh4h6nht4n1h0lr038x
```

- 资源记录定义的

```assembly
name [TTL] IN rr_type value
```

> 注意：
>
> 1. TTL可从全局继承
> 2. 使用 "@" 符号可用于引用当前区域的域名
> 3. 同一个名字可以通过多条记录定义多个不同的值；此时DNS服务器会以轮询方式响应
> 4. 同一个值也可能有多个不同的定义名字；通过多个不同的名字指向同一个值进行定义；此仅表示通 过多个不同的名字可以找到同一个主机



- DNS区域数据库文件

```bash
# named.conf配置文件中默认指定的数据路径下创建local-zone目录
mkdir -p /var/opt/isc/isc-bind/named/data/local-zone
# 保持源文件权限 或 手动授权：chmod 640 ${fileName} && chgrp named ${fileName}
cp -p /var/opt/isc/isc-bind/named/data/named.run /var/opt/isc/isc-bind/named/data/local-zone/localhost-forward.db
# 清空复制的新文件内容
cat /dev/null > /var/opt/isc/isc-bind/named/data/local-zone/localhost-forward.db
# 复制粘贴zone配置内容至DNS区域数据库文件
vim /var/opt/isc/isc-bind/named/data/local-zone/localhost-forward.db
```

```assembly
; TTL（Time To Live）：指定记录在 DNS 缓存中的存活时间。
; SOA 记录：用于定义区域的权威信息，如主服务器域名、联系人信息、刷新时间等。
; NS 记录：指定区域的名称服务器。
; A 记录：将域名映射到 IPv4 地址。

; 使用 "@" 符号可用于引用当前区域的域名
; Start of Authority RR defining the key characteristics of the zone (domain)
@        IN        SOA   main.dns.org. 77784423.qq.com. ( ; 设置DNS主服务器域名：main以及联系人信息
                         0       ; serial
                         1D      ; refresh
                         1H      ; retry
                         1W      ; expire
                         3H )    ; minimum

; name server RR for the domain
         IN        NS    main.dns.org.

; domain hosts includes NS and MX records defined above
; plus any others required
; for instance a user query for the A RR of joe.example.com will
; return the IPv4 address 192.168.254.6 from this zone file
main     IN        A     192.168.100.101 ; 映射DNS主服务器域名到 192.168.100.101

www      IN      CNAME   cdn.dns.org.    ; 映射 www. 域名前缀到 WEB 服务器的 IP 地址
cdn      IN        A     192.168.100.102 ; 映射 cdn. 域名前缀到 CDN 服务器的 IP 地址
db       IN        A     192.168.100.103 ; 映射 db. 域名前缀到 DB 服务器的 IP 地址

*        IN        A     192.168.100.102 ; 设置通配符 *（泛域名解析），容忍域名前缀错误
@        IN        A     192.168.100.102 ; 映射当前区域的域名到 WEB 服务器的 IP 地址

; 邮件服务器
@        MX        10    mail1;
@        MX        20    mail2;
mail1    IN        A     192.168.100.104 ;
mail2    IN        A     192.168.100.105 ;
```

> named.conf配置文件引用zones数据库文件

```bash
vim /etc/opt/isc/isc-bind/named.conf
```

```bash
include "/var/opt/isc/isc-bind/named/data/local-zone/localhost-forward.zones";
```

- 创建并配置zones数据库文件

```bash
vim /var/opt/isc/isc-bind/named/data/local-zone/localhost-forward.zones
```

```c++
// Provide forward mapping zone for localhost
// (optional)
// 配置域名：dns.org，注意：不要加前缀主机名称，如 www. 、 db. 等
zone "dns.org" {
  type master;
  file "local-zone/localhost-forward.db";
  notify no;
};
```

- DNS主服务器配置文件语法检查

```bash
named-checkconf
named-checkzone dns.org /var/opt/isc/isc-bind/named/data/local-zone/localhost-forward.db
```

- 配置生效（三种方式）

```bash
rndc reload
systemctl reload named
service named reload
```

## 四、客户端测试

- 客户端DNS配置主DNS服务器IP地址

```bash
vim /etc/sysconfig/network-scripts/ifcfg-eth0
```

```bash
DNS1=192.168.100.101
```

- 重启网卡

```bash
nmcli con reload
```

- DNS配置查看

```bash
cat /etc/resolv.conf
```

```bash
# Generated by NetworkManager
nameserver 192.168.100.101
```

- 相关工具测试

```bash
[root@localhost ~]# dig dns.org

; <<>> DiG 9.18.18 <<>> dns.org
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 7545
;; flags: qr aa rd ra; QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 1

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 1232
; COOKIE: 541ac04dde3e385a0100000064f0480be8100da42c4ed6f2 (good)
;; QUESTION SECTION:
;dns.org.			IN	A

;; ANSWER SECTION:
dns.org.		86400	IN	A	192.168.100.102

;; Query time: 1 msec
;; SERVER: 192.168.100.101#53(192.168.100.101) (UDP)
;; WHEN: Thu Aug 31 15:58:03 CST 2023
;; MSG SIZE  rcvd: 80
```

```bash
[root@localhost ~]# host dns.org
dns.org has address 192.168.100.102
[root@localhost ~]# host db.dns.org
db.dns.org has address 192.168.100.103
[root@localhost ~]# host www.dns.org
www.dns.org has address 192.168.100.102
```

```bash
[root@localhost ~]# ping dns.org -c 4
PING dns.org (192.168.100.102) 56(84) bytes of data.
64 bytes from 192.168.100.102 (192.168.100.102): icmp_seq=1 ttl=64 time=0.246 ms
64 bytes from 192.168.100.102 (192.168.100.102): icmp_seq=2 ttl=64 time=0.417 ms
64 bytes from 192.168.100.102 (192.168.100.102): icmp_seq=3 ttl=64 time=0.222 ms
64 bytes from 192.168.100.102 (192.168.100.102): icmp_seq=4 ttl=64 time=0.377 ms

--- dns.org ping statistics ---
4 packets transmitted, 4 received, 0% packet loss, time 3001ms
rtt min/avg/max/mdev = 0.222/0.315/0.417/0.085 ms
```

```bash
[root@localhost ~]# nslookup 
> server dns.org
Default server: dns.org
Address: 192.168.100.102#53
```

```bash
[root@localhost ~]# host www.dns.org
www.dns.org is an alias for cdn.dns.org.
cdn.dns.org has address 192.168.100.102
```

```bash
[root@localhost ~]# dig www.dns.org

; <<>> DiG 9.18.18 <<>> www.dns.org
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 20748
;; flags: qr aa rd ra; QUERY: 1, ANSWER: 2, AUTHORITY: 0, ADDITIONAL: 1

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 1232
; COOKIE: 7b14effc113548270100000064f156ecc02bfb28c332737a (good)
;; QUESTION SECTION:
;www.dns.org.			IN	A

;; ANSWER SECTION:
www.dns.org.		86400	IN	CNAME	cdn.dns.org.
cdn.dns.org.		86400	IN	A	192.168.100.102

;; Query time: 1 msec
;; SERVER: 127.0.0.1#53(127.0.0.1) (UDP)
;; WHEN: Fri Sep 01 11:13:48 CST 2023
;; MSG SIZE  rcvd: 102
```

## 五、实现反向DNS解析

> 使用场景较少，通常用于垃圾邮件检测

```bash
vim /var/opt/isc/isc-bind/named/data/local-zone/localhost-forward.zones
```

```assembly
zone "100.168.192.in-addr.arpa" {
  type master;
  file "local-zone/localhost-forward.db";
  notify no;
};
```

```bash
vim /var/opt/isc/isc-bind/named/data/local-zone/localhost-forward.db
```

```assembly
@        IN        SOA   main.dns.org. 77784423.qq.com. ( ; 设置DNS主服务器域名：main以及联系人信息
                         0       ; serial
                         1D      ; refresh
                         1H      ; retry
                         1W      ; expire
                         3H )    ; minimum
; name server RR for the domain
         IN        NS    main.dns.org.

100      IN       PTR    test.dns.org.  ;
200      IN       PTR    app.test.org. ;
```

- 验证配置是否生效

> dig -x 192.168.100.100 @your_dns_server_ip

```bash
[root@localhost ~]# dig -x 192.168.100.100 @192.168.100.101

; <<>> DiG 9.18.18 <<>> -x 192.168.100.100 @192.168.100.101
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 21714
;; flags: qr aa rd ra; QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 1

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 1232
; COOKIE: a9cf066b1fcb757b0100000064f19224ed0238bfa0e1f51b (good)
;; QUESTION SECTION:
;100.100.168.192.in-addr.arpa.	IN	PTR

;; ANSWER SECTION:
100.100.168.192.in-addr.arpa. 86400 IN	PTR	test.dns.org.

;; Query time: 0 msec
;; SERVER: 192.168.100.101#53(192.168.100.101) (UDP)
;; WHEN: Fri Sep 01 15:26:28 CST 2023
;; MSG SIZE  rcvd: 111
```

```
[root@localhost ~]# dig -x 192.168.100.200 @192.168.100.101

; <<>> DiG 9.18.18 <<>> -x 192.168.100.200 @192.168.100.101
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 6675
;; flags: qr aa rd ra; QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 1

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 1232
; COOKIE: 78a5e5ce1c4b291d0100000064f1923b343a011d52ad9190 (good)
;; QUESTION SECTION:
;200.100.168.192.in-addr.arpa.	IN	PTR

;; ANSWER SECTION:
200.100.168.192.in-addr.arpa. 86400 IN	PTR	app.test.org.

;; Query time: 0 msec
;; SERVER: 192.168.100.101#53(192.168.100.101) (UDP)
;; WHEN: Fri Sep 01 15:26:51 CST 2023
;; MSG SIZE  rcvd: 111
```

## 六、启用DNS客户端缓存功能

> 在高并发的服务器场景中，对DNS的服务器查询性能有较高的要求,如果在客户端启用DNS缓存功能，可以大幅减轻DNS服务器的压力，同时也能提高DNS客户端名称解析速度

### 6.1 CentOS 启用DNS客户端缓存

> Windows默认有DNS缓存，CentOS 默认没有启用DNS客户端缓存，安装nscd（Name Service Cache Daemon,名称服务缓存守护进 程）包可以支持DNS缓存功能
>
> 减少DNS服务器压力,提高DNS查询速度

```bash
yum -y install nscd
systemctl enable --now nscd
```

- 查看缓存统计信息

```bash
nscd -g
```

```bash
nscd configuration:

              0  server debug level
            26s  server runtime
              5  current number of threads
             32  maximum number of threads
              0  number of times clients had to wait
             no  paranoia mode enabled
           3600  restart internal
              5  reload count

passwd cache:

            yes  cache is enabled
            yes  cache is persistent
            yes  cache is shared
            211  suggested size
         216064  total data pool size
              0  used data pool size
            600  seconds time to live for positive entries
             20  seconds time to live for negative entries
              0  cache hits on positive entries
              0  cache hits on negative entries
              0  cache misses on positive entries
              0  cache misses on negative entries
              0% cache hit rate
              0  current number of cached values
              0  maximum number of cached values
              0  maximum chain length searched
              0  number of delays on rdlock
              0  number of delays on wrlock
              0  memory allocations failed
            yes  check /etc/passwd for changes

group cache:

            yes  cache is enabled
            yes  cache is persistent
            yes  cache is shared
            211  suggested size
         216064  total data pool size
              0  used data pool size
           3600  seconds time to live for positive entries
             60  seconds time to live for negative entries
              0  cache hits on positive entries
              0  cache hits on negative entries
              0  cache misses on positive entries
              0  cache misses on negative entries
              0% cache hit rate
              0  current number of cached values
              0  maximum number of cached values
              0  maximum chain length searched
              0  number of delays on rdlock
              0  number of delays on wrlock
              0  memory allocations failed
            yes  check /etc/group for changes

hosts cache:

            yes  cache is enabled
            yes  cache is persistent
            yes  cache is shared
            211  suggested size
         216064  total data pool size
            208  used data pool size
           3600  seconds time to live for positive entries
             20  seconds time to live for negative entries
              0  cache hits on positive entries
              0  cache hits on negative entries
              1  cache misses on positive entries
              1  cache misses on negative entries
              0% cache hit rate
              2  current number of cached values
              2  maximum number of cached values
              0  maximum chain length searched
              0  number of delays on rdlock
              0  number of delays on wrlock
              0  memory allocations failed
            yes  check /etc/hosts for changes

services cache:

            yes  cache is enabled
            yes  cache is persistent
            yes  cache is shared
            211  suggested size
         216064  total data pool size
              0  used data pool size
          28800  seconds time to live for positive entries
             20  seconds time to live for negative entries
              0  cache hits on positive entries
              0  cache hits on negative entries
              0  cache misses on positive entries
              0  cache misses on negative entries
              0% cache hit rate
              0  current number of cached values
              0  maximum number of cached values
              0  maximum chain length searched
              0  number of delays on rdlock
              0  number of delays on wrlock
              0  memory allocations failed
            yes  check /etc/services for changes

netgroup cache:

            yes  cache is enabled
            yes  cache is persistent
            yes  cache is shared
            211  suggested size
         216064  total data pool size
              0  used data pool size
          28800  seconds time to live for positive entries
             20  seconds time to live for negative entries
              0  cache hits on positive entries
              0  cache hits on negative entries
              0  cache misses on positive entries
              0  cache misses on negative entries
              0% cache hit rate
              0  current number of cached values
              0  maximum number of cached values
              0  maximum chain length searched
              0  number of delays on rdlock
              0  number of delays on wrlock
              0  memory allocations failed
            yes  check /etc/netgroup for changes
```

- 清除DNS客户端缓存

```bash
nscd -i hosts
```

## 七、实现从DNS服务器

> 参考 第一、第二章节 完成DNS相关工具的安装与配置

```bash
# named.conf配置文件中默认指定的数据路径下创建slaves目录
mkdir -p /var/opt/isc/isc-bind/named/data/slaves
chown named:named /var/opt/isc/isc-bind/named/data/slaves
```

> named.conf配置文件引用zones数据库文件

```bash
vim /etc/opt/isc/isc-bind/named.conf
```

```bash
include "/var/opt/isc/isc-bind/named/data/slaves/localhost-forward.zones";
```

- 创建并配置zones数据库文件

```bash
vim /var/opt/isc/isc-bind/named/data/slaves/localhost-forward.zones
```

```c++
// 同步主DNS服务器设置的域名
zone "dns.org" {
  // 设置从DNS服务器zone类型为slave
  type slave;
  // 设置同步的主DNS服务器的IP地址
  masters {192.168.100.101};
  // 设置同步的主DNS服务器的数据库配置路径
  file "slaves/localhost-forward.db"
  notify no;
};
```

- **修改主DNS服务器的数据库配置**，保证从节点实时同步主DNS服务器修改的数据库文件内容

> 注意：修改 serial 版本号+1

```bash
vim /var/opt/isc/isc-bind/named/data/local-zone/localhost-forward.db
```

```assembly
; TTL（Time To Live）：指定记录在 DNS 缓存中的存活时间。
; SOA 记录：用于定义区域的权威信息，如主服务器域名、联系人信息、刷新时间等。
; NS 记录：指定区域的名称服务器。
; A 记录：将域名映射到 IPv4 地址。

; 使用 "@" 符号可用于引用当前区域的域名
; Start of Authority RR defining the key characteristics of the zone (domain)
@        IN        SOA   main.dns.org. 77784423.qq.com. ( ; 设置DNS主服务器域名：main以及联系人信息
                         1       ; serial
                         1D      ; refresh
                         1H      ; retry
                         1W      ; expire
                         3H )    ; minimum

; name server RR for the domain
         IN        NS    main.dns.org.
         IN        NS    slave01.dns.org.

; domain hosts includes NS and MX records defined above
; plus any others required
; for instance a user query for the A RR of joe.example.com will
; return the IPv4 address 192.168.254.6 from this zone file
main     IN        A     192.168.100.101 ; 映射DNS主服务器域名到 192.168.100.101
slave01  IN        A     192.168.100.114 ; 映射DNS从服务器域名到 192.168.100.114

…………………………………………………… ; 省略内容
```

- 黑客小技巧：获取DNS域名下的DNS配置列表

```bash
[root@localhost ~]# dig -t axfr dns.org

; <<>> DiG 9.18.18 <<>> -t axfr dns.org
;; global options: +cmd
dns.org.		86400	IN	SOA	main.dns.org. 77784423.qq.com. 0 86400 3600 604800 10800
dns.org.		86400	IN	A	192.168.100.102
dns.org.		86400	IN	MX	10 mail1.dns.org.
dns.org.		86400	IN	MX	20 mail2.dns.org.
dns.org.		86400	IN	NS	main.dns.org.
*.dns.org.		86400	IN	A	192.168.100.102
100.dns.org.		86400	IN	PTR	test.dns.org.
200.dns.org.		86400	IN	PTR	app.test.org.
cdn.dns.org.		86400	IN	A	192.168.100.102
db.dns.org.		86400	IN	A	192.168.100.103
mail1.dns.org.		86400	IN	A	192.168.100.104
mail2.dns.org.		86400	IN	A	192.168.100.105
main.dns.org.		86400	IN	A	192.168.100.101
www.dns.org.		86400	IN	CNAME	cdn.dns.org.
dns.org.		86400	IN	SOA	main.dns.org. 77784423.qq.com. 0 86400 3600 604800 10800
;; Query time: 2 msec
;; SERVER: 127.0.0.1#53(127.0.0.1) (TCP)
;; WHEN: Fri Sep 01 16:50:19 CST 2023
;; XFR size: 15 records (messages 1, bytes 406)
```

- **修改所有主DNS服务器的named配置，设置`allow-transfer`参数，提升DNS服务器安全性**

```
vim /etc/opt/isc/isc-bind/named.conf
```

```assembly
options {
        directory "/var/opt/isc/isc-bind/named/data";
        // 设置listen为主机所有IP
        listen-on { localhost; };
        listen-on-v6 { ::1; };
        dnssec-validation auto;
        // 设置允许从DNS服务器的IP地址进行区域传输
        allow-transfer {192.168.100.114;};
};

…………………………………………………… ; 省略内容
```

- **修改所有从DNS服务器的named配置，设置`allow-transfer`参数，提升DNS服务器安全性**

```
vim /etc/opt/isc/isc-bind/named.conf
```

```assembly
options {
        directory "/var/opt/isc/isc-bind/named/data";
        // 设置listen为主机所有IP
        listen-on { localhost; };
        listen-on-v6 { ::1; };
        dnssec-validation auto;
        // 设置不允许其它主机进行区域传输
        allow-transfer {none;};
};

…………………………………………………… ; 省略内容
```



## 文章参考

- https://bind9.readthedocs.io/en/latest/chapter3.html#
- https://bind9.readthedocs.io/en/latest/reference.html#namedconf-statement-zone
- https://access.redhat.com/documentation/zh-cn/red_hat_enterprise_linux/9/html/managing_networking_infrastructure_services/assembly_setting-up-and-configuring-a-bind-dns-server_networking-infrastructure-services
