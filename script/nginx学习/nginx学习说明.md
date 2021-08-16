### 浏览器请求
​	nginx转发
​		http://localhost:8088/hello/url
​	直接请求
​		http://localhost:9000/hello/url
### 启动jar
​	java -jar springbootdemo.jar --server.port=9000
​    java -jar springbootdemo.jar --server.port=9001
​    java -jar springbootdemo.jar --server.port=9002
​    或者:
​	java -Dfile.encoding=utf-8 -jar springbootdemo-1.0-SNAPSHOT.jar --server.port=9000
​	(-Dfile.encoding=utf-8 作用: 保证启动banner中文无乱码)
cmd中文乱码解决
​	输入chcp命令:  chcp 65001
​	65001 == utf-8
​	936 == gbk

### nginx常用命令

基于nginx-1.18.0

```shell
#启动
start nginx
#优雅停止
nginx -s quit 
#停止
nginx -s stop 
#重新加载配置文件
nginx -s reload 
#查看nginx进程
ps aux |grep nginx 
```

### 学习文章:
​	https://www.kuangstudy.com/bbs/1382150642935652354