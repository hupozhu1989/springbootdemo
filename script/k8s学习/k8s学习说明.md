## 一、yaml文件

### 1. 一个容器组成的 Pod 的 yaml 示例

```yaml
apiVersion: v1 
kind: Pod 
metadata:
  name: mytomcat 
  labels:
    name: mytomcat 
spec:
  containers:
  - name: mytomcat 
    image: tomcat 
    ports:
    - containerPort: 8000
```

### 2. 多个容器组成的 Pod 的 yaml 示例

```yaml
apiVersion: v1 
kind: Pod 
metadata:
  name: myweb 
  labels:
    name: tomcat-redis
spec:
  containers:
  - name: tomcat 
    image: tomcat 
    ports:
    - containerPort: 8080
  - name: redis 
    image: redis 
    ports:
    - containerPort: 6379
```

### 3. 资源限制示例

```yaml
apiVersion: v1 
kind: Pod 
metadata:
  name: frontend
sepc:
  containers:
  - name: db
    image: mysql
    env:
   - name: MYSQL_ROOT_PASSWORD
     value: "password"     
    resources:
      # 该资源最小申请数量
      requests:
        memory: "64Mi"
        cpu: "250m"
      # 该资源最大允许使用的量
      limits:
        memory: "128Mi"
        cpu: "500m"
```

### 4. Pod重启策略

```yaml
apiVersion: v1 
kind: Pod 
metadata:
  name: dns-test 
spec:
  containers:
  - name: busybox 
    image: busybox:1.28.4
    args:
    - /bin/sh
    - -c
    - sleep 36000
  #Pod重启策略:Always:默认值,当容器终止退出后,总是重启容器;OnFailure:当容器异常退出(退出状态码非0)时,才重启容器;Never:当容器终止退出,从不重启容器
  restartPolicy: Never
```

### 5. Pod健康检查

```yaml
apiVersion: v1 
kind: Pod 
metadata:
  labels: 
    test: liveness
  name: liveness-exec
spec:
  containers:
  - name: liveness 
    image: busybox:1.28.4
    args:
    - /bin/sh
    - -c
    - touch /tmp/healthy; sleep 30; rm -rf /tmp/healthy
    #存活检查:如果检查失败,将杀死容器,根据Pod的restartPolicy来操作
    liveenessProbe:
      exec:
        commond:
        - cat
        - /tmp/healthy
      initialDelaySeconds: 5
      periodSeconds: 5
```

### 6. Pod调度

①Pod资源限制对Pod调用产生影响,参考上面【资源限制示例】

②节点选择器标签影响Pod调度

```yaml
#首先对节点创建标签
kubectl label node1 env_role=dev

spec:
  nodeSelector:
    env_role: dev 
```

③节点亲和性影响Pod调度

todo

④污点和污点容忍

todo

```yaml
#查看污点情况
kubectl describe node k8smaster |grep Taint
#为节点添加污点
kubectl taint node [nodeName] key=value:污点三个值
#删除污点
kubectl taint node k8snode1 env_role:NoSchedule-

spec:
  tolerations:
  - key: "key"
    operator: "Equal"
    value: "value"
    effect: "NoSchedule"  
```

## 二、controller

### 1. Deployment-无状态

```shell
#准备工作
docker pull nginx:1.18.0
docker pull nginx:1.20.0
#第一步 导出deploy的yaml文件
kubectl create deployment nginx-deploy --image=nginx:1.18.0 --dry-run=client -o yaml > deploy01.yaml
#第二步 使用yaml部署
kubectl apply -f deploy01.yaml
#第三步 对外发布(暴露对外端口号)
kubectl expose deployment nginx-deploy --port=80 --target-port=80 --type=NodePort --name=nginx-svc -o yaml > deploy02.yaml

#查看部署状态  想看更多后面加 -o wide
kubectl get deploy
kubectl get pod,svc
#应用升级回滚和弹性伸缩
#应用升级
kubectl set image deployment nginx-deploy nginx=nginx:1.20.0
#查看升级版本
kubectl rollout history deployment nginx-deploy
#查看升级状态
kubectl rollout status deployment nginx-deploy
#回滚到上一个版本
kubectl rollout undo deployment nginx-deploy
#回滚到指定版本
kubectl rollout undo deployment nginx-deploy --to-revision=1
#弹性伸缩
kubectl scale deployment nginx-deploy --replicas=3
```

### 2. StatefulSet-有状态

statefulset.yaml

```yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx
  labels: 
    app: nginx
spec:
  ports: 
  - port: 80
    name: web
  #无头service
  clusterIP: None
  selector: 
    app: nginx
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: nginx-statefulset
  namespace: default
spec:
  serviceName: nginx
  replicas: 3 
  selector: 
    matchLabels:
      app: nginx
  template:
    metadata:
      labels: 
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.20.0
        ports:
        - containerPort: 80
```

```shell
kubectl get statefulset
kubectl get pod,svc
kubectl delete statefulset [set名字]
kubectl delete statefulset --all
```

### 3. DaemonSet-守护进程

daemonset.yaml

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: nginx-daemonset
  labels: 
    app: filebeat
spec:
  selector:
    matchLabels:
      app: filebeat
  template:
    metadata:
      labels: 
        app: filebeat
    spec:
      containers:
      - name: logs
        image: nginx:1.20.0
        ports:
        - containerPort: 80
        volumeMounts:
        - name: varlog
          mountPath: /tmp/log
      volumes:
      - name: varlog
        hostPath:
          path: /var/log
```

```shell
kubectl get daemonset
kubectl exec -it nginx-daemonset-9cs94 bash
ls /tmp/log
exit
```

### 4. Job-一次性任务

job.yaml

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: pi-job
  labels: 
    app: filebeat
spec:
  template:
    spec:
      containers:
      - name: pi
        image: perl
        command: ["perl", "-Mbignum=bpi", "-wle", "print bpi(2000)"]
      restartPolicy: Never
  backoffLimit: 5
```

```shell
kubectl get job
kubectl logs -f pi-job-5nwf6
```

### 5. CronJob-定时任务

cronjob.yaml

```yaml
apiVersion: batch/v1beta1
kind: CronJob
metadata:
  name: busybox-cronjob
spec:
  schedule: "*/1 * * * *"
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: busybox
            image: busybox
            args:
            - /bin/sh
            - -c
            - date; echo "有理有据,以人为本"
          restartPolicy: OnFailure
```

```shell
kubectl get cronjob
kubectl logs -f busybox-cronjob-1628949240-zz29x
```

## 三、配置管理

### 1. Secret

mysecret.yaml

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: mysecret
type: Opaque
data:
  username: YWRtaW4=
  password: MWYyZDFlMmU2N2Rm
```

#### 以变量形式挂载到pod容器中

secret-var.yaml

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secret-var-pod
spec:
  containers:
  - name: nginx
    image: nginx:1.20.0
    env:
      - name: SECRET_USERNAME
        valueFrom:
          secretKeyRef:
            name: mysecret
            key: username
      - name: SECRET_PASSWORD
        valueFrom:
          secretKeyRef:
            name: mysecret
            key: password
```

```shell
kubectl exec -it secret-var-pod bash
echo $SECRET_USERNAME
echo $SECRET_PASSWORD
exit
```

#### 以Volume形式挂载到pod容器中

secret-vol.yaml

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secret-vol-pod
spec:
  containers:
  - name: nginx
    image: nginx:1.20.0
    volumeMounts:
    - name: foo
      mountPath: "/etc/foo"
      readOnly: true
  volumes:
  - name: foo
    secret:
      secretName: mysecret
```

```shell
kubectl exec -it secret-vol-pod bash
ls /etc/foo
cat /etc/foo/password
cat /etc/foo/username 
```

### 2. ConfigMap

#### 创建配置文件

redis-cm.properties

```properties
redis.host=127.0.0.1
redis.port=6379
redis.password=123456
```

#### 创建configmap

```shell
kubectl create configmap redis-cm --from-file=redis-cm.properties
kubectl get cm
kubectl describe cm redis-cm
#导出redis-cm.yaml文件
kubectl get cm redis-cm -o=yaml > redis-cm.yaml
```

redis-cm.yaml如下:

```yaml
apiVersion: v1
data:
  redis-cm.properties: |
    redis.host=127.0.0.1
    redis.port=6379
    redis.password=123456
kind: ConfigMap
metadata:
  creationTimestamp: "2021-08-16T03:51:51Z"
  managedFields:
  - apiVersion: v1
    fieldsType: FieldsV1
    fieldsV1:
      f:data:
        .: {}
        f:redis-cm.properties: {}
    manager: kubectl-create
    operation: Update
    time: "2021-08-16T03:51:51Z"
  name: redis-cm
  namespace: default
  resourceVersion: "183687"
  uid: 16a53c6e-b02e-4db5-ac3b-4ca63e4a4761
```

#### 以Volume挂载到pod容器中

configmap-vol.yaml

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: cm-vol-pod
spec:
  containers:
    - name: busybox
      image: busybox
      command: [ "/bin/sh","-c","cat /etc/config/redis-cm.properties" ]
      volumeMounts:
      - name: config-volume
        mountPath: /etc/config
  volumes:
    - name: config-volume
      configMap:
        name: redis-cm
  restartPolicy: Never
```

```shell
kubectl logs -f pod/cm-vol-pod
```

#### 以变量形式挂载到pod容器

myconfigmap.yaml

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: myconfigmap
  namespace: default
data:
  special.level: info
  special.type: hello
```

configmap-var.yaml

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: cm-var-pod
spec:
  containers:
    - name: busybox
      image: busybox
      command: [ "/bin/sh", "-c", "echo $(LEVEL) $(TYPE)" ]
      env:
        - name: LEVEL
          valueFrom:
            configMapKeyRef:
              name: myconfigmap
              key: special.level
        - name: TYPE
          valueFrom:
            configMapKeyRef:
              name: myconfigmap
              key: special.type
  restartPolicy: Never
```

```shell
kubectl logs -f cm-var-pod
```

## 四、集群安全机制

kubectl get **ns**





## 五、Ingress

### ①创建nginx应用，对外暴露端口使用NodePort

```shell
kubectl create deployment nginx-web --image=nginx:1.18.0
kubectl expose deployment nginx-web --port=80 --target-port=80 --type=NodePort

```

### ②部署ingress-controller

ingress-controller.yaml

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ingress-nginx
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: nginx-configuration
  namespace: ingress-nginx
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: tcp-services
  namespace: ingress-nginx
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: udp-services
  namespace: ingress-nginx
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: nginx-ingress-serviceaccount
  namespace: ingress-nginx
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
---
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: ClusterRole
metadata:
  name: nginx-ingress-clusterrole
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
rules:
  - apiGroups:
      - ""
    resources:
      - configmaps
      - endpoints
      - nodes
      - pods
      - secrets
    verbs:
      - list
      - watch
  - apiGroups:
      - ""
    resources:
      - nodes
    verbs:
      - get
  - apiGroups:
      - ""
    resources:
      - services
    verbs:
      - get
      - list
      - watch
  - apiGroups:
      - ""
    resources:
      - events
    verbs:
      - create
      - patch
  - apiGroups:
      - "extensions"
      - "networking.k8s.io"
    resources:
      - ingresses
    verbs:
      - get
      - list
      - watch
  - apiGroups:
      - "extensions"
      - "networking.k8s.io"
    resources:
      - ingresses/status
    verbs:
      - update
---
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: Role
metadata:
  name: nginx-ingress-role
  namespace: ingress-nginx
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
rules:
  - apiGroups:
      - ""
    resources:
      - configmaps
      - pods
      - secrets
      - namespaces
    verbs:
      - get
  - apiGroups:
      - ""
    resources:
      - configmaps
    resourceNames:
      # Defaults to "<election-id>-<ingress-class>"
      # Here: "<ingress-controller-leader>-<nginx>"
      # This has to be adapted if you change either parameter
      # when launching the nginx-ingress-controller.
      - "ingress-controller-leader-nginx"
    verbs:
      - get
      - update
  - apiGroups:
      - ""
    resources:
      - configmaps
    verbs:
      - create
  - apiGroups:
      - ""
    resources:
      - endpoints
    verbs:
      - get
---
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: RoleBinding
metadata:
  name: nginx-ingress-role-nisa-binding
  namespace: ingress-nginx
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: nginx-ingress-role
subjects:
  - kind: ServiceAccount
    name: nginx-ingress-serviceaccount
    namespace: ingress-nginx
---
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: ClusterRoleBinding
metadata:
  name: nginx-ingress-clusterrole-nisa-binding
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: nginx-ingress-clusterrole
subjects:
  - kind: ServiceAccount
    name: nginx-ingress-serviceaccount
    namespace: ingress-nginx
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-ingress-controller
  namespace: ingress-nginx
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: ingress-nginx
      app.kubernetes.io/part-of: ingress-nginx
  template:
    metadata:
      labels:
        app.kubernetes.io/name: ingress-nginx
        app.kubernetes.io/part-of: ingress-nginx
      annotations:
        prometheus.io/port: "10254"
        prometheus.io/scrape: "true"
    spec:
      hostNetwork: true
      # wait up to five minutes for the drain of connections
      terminationGracePeriodSeconds: 300
      serviceAccountName: nginx-ingress-serviceaccount
      nodeSelector:
        kubernetes.io/os: linux
      containers:
        - name: nginx-ingress-controller
          image: lizhenliang/nginx-ingress-controller:0.30.0
          args:
            - /nginx-ingress-controller
            - --configmap=$(POD_NAMESPACE)/nginx-configuration
            - --tcp-services-configmap=$(POD_NAMESPACE)/tcp-services
            - --udp-services-configmap=$(POD_NAMESPACE)/udp-services
            - --publish-service=$(POD_NAMESPACE)/ingress-nginx
            - --annotations-prefix=nginx.ingress.kubernetes.io
          securityContext:
            allowPrivilegeEscalation: true
            capabilities:
              drop:
                - ALL
              add:
                - NET_BIND_SERVICE
            # www-data -> 101
            runAsUser: 101
          env:
            - name: POD_NAME
              valueFrom:
                fieldRef:
                  fieldPath: metadata.name
            - name: POD_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
          ports:
            - name: http
              containerPort: 80
              protocol: TCP
            - name: https
              containerPort: 443
              protocol: TCP
          livenessProbe:
            failureThreshold: 3
            httpGet:
              path: /healthz
              port: 10254
              scheme: HTTP
            initialDelaySeconds: 10
            periodSeconds: 10
            successThreshold: 1
            timeoutSeconds: 10
          readinessProbe:
            failureThreshold: 3
            httpGet:
              path: /healthz
              port: 10254
              scheme: HTTP
            periodSeconds: 10
            successThreshold: 1
            timeoutSeconds: 10
          lifecycle:
            preStop:
              exec:
                command:
                  - /wait-shutdown
---
apiVersion: v1
kind: LimitRange
metadata:
  name: ingress-nginx
  namespace: ingress-nginx
  labels:
    app.kubernetes.io/name: ingress-nginx
    app.kubernetes.io/part-of: ingress-nginx
spec:
  limits:
  - min:
      memory: 90Mi
      cpu: 100m
    type: Container
```

```shell
kubectl apply -f ingress-controller.yaml
#执行结果：
namespace/ingress-nginx created
configmap/nginx-configuration created
configmap/tcp-services created
configmap/udp-services created
serviceaccount/nginx-ingress-serviceaccount created
Warning: rbac.authorization.k8s.io/v1beta1 ClusterRole is deprecated in v1.17+, unavailable in v1.22+; use rbac.authorization.k8s.io/v1 ClusterRole
clusterrole.rbac.authorization.k8s.io/nginx-ingress-clusterrole created
Warning: rbac.authorization.k8s.io/v1beta1 Role is deprecated in v1.17+, unavailable in v1.22+; use rbac.authorization.k8s.io/v1 Role
role.rbac.authorization.k8s.io/nginx-ingress-role created
Warning: rbac.authorization.k8s.io/v1beta1 RoleBinding is deprecated in v1.17+, unavailable in v1.22+; use rbac.authorization.k8s.io/v1 RoleBinding
rolebinding.rbac.authorization.k8s.io/nginx-ingress-role-nisa-binding created
Warning: rbac.authorization.k8s.io/v1beta1 ClusterRoleBinding is deprecated in v1.17+, unavailable in v1.22+; use rbac.authorization.k8s.io/v1 ClusterRoleBinding
clusterrolebinding.rbac.authorization.k8s.io/nginx-ingress-clusterrole-nisa-binding created
deployment.apps/nginx-ingress-controller created
limitrange/ingress-nginx created

#查看ingress controller状态
kubectl get pods -n ingress-nginx -o wide
```

### ③创建ingress规则

example-ingress.yaml

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: example-ingress
spec:
  rules:
  - host: example.ingredemo.com
    http:
      paths:
      - path: /
        backend:
          serviceName: nginx-web
          servicePort: 80
```

```shell
kubectl get ing
#在相应node节点执行
netstat -antp | grep 80
netstat -antp | grep 443
```

### ④windows系统配置host

C:\Windows\System32\drivers\etc\

192.168.31.62  example.ingredemo.com

浏览器访问:  example.ingredemo.com

## 六、Helm

①helm安装

```shell
tar zxvf helm-v3.0.0-linux-amd64.tar.gz
cd linux-amd64/
mv helm /usr/bin
```

②配置helm仓库

```shell
#添加存储库
helm repo add stable http://mirror.azure.cn/kubernetes/charts
helm repo add aliyun https://kubernetes.oss-cn-hangzhou.aliyuncs.com/charts
#更新仓库
helm repo update
#查看配置的存储库
helm repo list
helm search repo stable
#删除存储库
helm repo remove aliyun
```

③使用helm快速部署应用  todo

```shell
#第一步 使用命令搜索应用
helm search repo weave
#第二步 根据搜索内容选择安装
helm install weave-ui aliyun/weave-scope
#查看安装之后状态
helm list
helm statu [安装之后名称]

kubectl edit svc weave-ui-scope
更改type,ClusterIP -> NodePort
```

④自己创建chart   todo

```shell
#1.使用命令创建chart
helm create mychart
#2.在templates文件夹创建2个yaml文件
#先删掉templates目录下所有文件
rm -rf *
kubectl create deployment nginx-chart --image=nginx:1.20.0 --dry-run=client -o yaml > deployment.yaml
kubectl apply -f deployment.yaml
kubectl expose deployment nginx-chart --port=80 --target-port=80 --type=NodePort --dry-run=client -o yaml > service.yaml
#3.安装mychart
helm install chartdemo mychart/
#4.应用升级
helm upgrade chart [名称]
```



## 七、持久化存储

#### 1.nfs-网络存储  todo

```shell
#第一步:找一台服务器作为nfs服务器   192.168.31.100
#①安装nfs
yum install -y nfs-utils
#②设置挂载路径
vi /etc/exports
/data/nfs *(rw,no_root_squash)
#③挂载路径需要创建出来
mkdir data
mkdir nfs
#第二步:在k8s集群node节点安装nfs,node1和node2
yum install -y nfs-utils
#第三步:在nfs服务器启动nfs服务
systemctl start nfs
ps -ef|grep [n]fs
#第四步:在k8s集群部署应用使用nfs持久网络存储
kubectl apply -f nginx-nfs.yaml
kubectl describe pod nginx-nfs-56545c6787-5nw5k
kubectl exec -it pod/nginx-nfs-56545c6787-5nw5k bash
ls /usr/share/nginx/html
```

nginx-nfs.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-nfs
spec:
  replicas: 1
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.20.0
        volumeMounts:
        - name: wwwroot
          mountPath: /usr/share/nginx/html
        ports:
        - containerPort: 80
      volumes:
        - name: wwwroot
          nfs:
            server: 192.168.31.100
            path: /data/nfs
```

#### 2.pv和pvc

pv.yaml   todo

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: my-pv
spec:
  capacity:
    storage: 2Gi
  accessModes:
    - ReadWriteMany
  nfs:
    path: /data/nfs
    server: 192.168.31.100
```

pvc.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-pvc
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.20.0
        volumeMounts:
        - name: wwwroot
          mountPath: /usr/share/nginx/html
        ports:
        - containerPort: 80
      volumes:
      - name: wwwroot
        persistentVolumeClaim:
          claimName: my-pvc
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: my-pvc
spec:
  accessModes:
    - ReadWriteMany
  resources:
    requests:
      storage: 2Gi
```

```shell
kubectl apply -f pv.yaml 
kubectl apply -f pvc.yaml
kubectl get pv,pvc
```

## 八、监控

#### 第一步 部署Prometheus

**部署守护进程**

###### node-exporter.yaml

```yaml
---
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: node-exporter
  namespace: kube-system
  labels:
    k8s-app: node-exporter
spec:
  selector:
    matchLabels:
      k8s-app: node-exporter
  template:
    metadata:
      labels:
        k8s-app: node-exporter
    spec:
      containers:
      - image: prom/node-exporter
        name: node-exporter
        ports:
        - containerPort: 9100
          protocol: TCP
          name: http
---
apiVersion: v1
kind: Service
metadata:
  labels:
    k8s-app: node-exporter
  name: node-exporter
  namespace: kube-system
spec:
  ports:
  - name: http
    port: 9100
    nodePort: 31672
    protocol: TCP
  type: NodePort
  selector:
    k8s-app: node-exporter
```

```shell
kubectl create -f node-exporter.yaml
#结果
daemonset.apps/node-exporter created
service/node-exporter created
```

**部署其他yaml文件**

###### rbac-setup.yaml

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: prometheus
rules:
- apiGroups: [""]
  resources:
  - nodes
  - nodes/proxy
  - services
  - endpoints
  - pods
  verbs: ["get", "list", "watch"]
- apiGroups:
  - extensions
  resources:
  - ingresses
  verbs: ["get", "list", "watch"]
- nonResourceURLs: ["/metrics"]
  verbs: ["get"]
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: prometheus
  namespace: kube-system
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: prometheus
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: prometheus
subjects:
- kind: ServiceAccount
  name: prometheus
  namespace: kube-system
```

```shell
kubectl create -f rbac-setup.yaml
#结果
clusterrole.rbac.authorization.k8s.io/prometheus created
serviceaccount/prometheus created
clusterrolebinding.rbac.authorization.k8s.io/prometheus created
```

###### configmap.yaml

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: kube-system
data:
  prometheus.yml: |
    global:
      scrape_interval:     15s
      evaluation_interval: 15s
    scrape_configs:

    - job_name: 'kubernetes-apiservers'
      kubernetes_sd_configs:
      - role: endpoints
      scheme: https
      tls_config:
        ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
      bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token
      relabel_configs:
      - source_labels: [__meta_kubernetes_namespace, __meta_kubernetes_service_name, __meta_kubernetes_endpoint_port_name]
        action: keep
        regex: default;kubernetes;https

    - job_name: 'kubernetes-nodes'
      kubernetes_sd_configs:
      - role: node
      scheme: https
      tls_config:
        ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
      bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token
      relabel_configs:
      - action: labelmap
        regex: __meta_kubernetes_node_label_(.+)
      - target_label: __address__
        replacement: kubernetes.default.svc:443
      - source_labels: [__meta_kubernetes_node_name]
        regex: (.+)
        target_label: __metrics_path__
        replacement: /api/v1/nodes/${1}/proxy/metrics

    - job_name: 'kubernetes-cadvisor'
      kubernetes_sd_configs:
      - role: node
      scheme: https
      tls_config:
        ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
      bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token
      relabel_configs:
      - action: labelmap
        regex: __meta_kubernetes_node_label_(.+)
      - target_label: __address__
        replacement: kubernetes.default.svc:443
      - source_labels: [__meta_kubernetes_node_name]
        regex: (.+)
        target_label: __metrics_path__
        replacement: /api/v1/nodes/${1}/proxy/metrics/cadvisor

    - job_name: 'kubernetes-service-endpoints'
      kubernetes_sd_configs:
      - role: endpoints
      relabel_configs:
      - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_scheme]
        action: replace
        target_label: __scheme__
        regex: (https?)
      - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_service_annotation_prometheus_io_port]
        action: replace
        target_label: __address__
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
      - action: labelmap
        regex: __meta_kubernetes_service_label_(.+)
      - source_labels: [__meta_kubernetes_namespace]
        action: replace
        target_label: kubernetes_namespace
      - source_labels: [__meta_kubernetes_service_name]
        action: replace
        target_label: kubernetes_name

    - job_name: 'kubernetes-services'
      kubernetes_sd_configs:
      - role: service
      metrics_path: /probe
      params:
        module: [http_2xx]
      relabel_configs:
      - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_probe]
        action: keep
        regex: true
      - source_labels: [__address__]
        target_label: __param_target
      - target_label: __address__
        replacement: blackbox-exporter.example.com:9115
      - source_labels: [__param_target]
        target_label: instance
      - action: labelmap
        regex: __meta_kubernetes_service_label_(.+)
      - source_labels: [__meta_kubernetes_namespace]
        target_label: kubernetes_namespace
      - source_labels: [__meta_kubernetes_service_name]
        target_label: kubernetes_name

    - job_name: 'kubernetes-ingresses'
      kubernetes_sd_configs:
      - role: ingress
      relabel_configs:
      - source_labels: [__meta_kubernetes_ingress_annotation_prometheus_io_probe]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_ingress_scheme,__address__,__meta_kubernetes_ingress_path]
        regex: (.+);(.+);(.+)
        replacement: ${1}://${2}${3}
        target_label: __param_target
      - target_label: __address__
        replacement: blackbox-exporter.example.com:9115
      - source_labels: [__param_target]
        target_label: instance
      - action: labelmap
        regex: __meta_kubernetes_ingress_label_(.+)
      - source_labels: [__meta_kubernetes_namespace]
        target_label: kubernetes_namespace
      - source_labels: [__meta_kubernetes_ingress_name]
        target_label: kubernetes_name

    - job_name: 'kubernetes-pods'
      kubernetes_sd_configs:
      - role: pod
      relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: ([^:]+)(?::\d+)?;(\d+)
        replacement: $1:$2
        target_label: __address__
      - action: labelmap
        regex: __meta_kubernetes_pod_label_(.+)
      - source_labels: [__meta_kubernetes_namespace]
        action: replace
        target_label: kubernetes_namespace
      - source_labels: [__meta_kubernetes_pod_name]
        action: replace
        target_label: kubernetes_pod_name
```

```shell
kubectl create -f configmap.yaml
#结果
configmap/prometheus-config created
```

###### prometheus.deploy.yml

```yaml
---
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    name: prometheus-deployment
  name: prometheus
  namespace: kube-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: prometheus
  template:
    metadata:
      labels:
        app: prometheus
    spec:
      containers:
      - image: prom/prometheus:v2.0.0
        name: prometheus
        command:
        - "/bin/prometheus"
        args:
        - "--config.file=/etc/prometheus/prometheus.yml"
        - "--storage.tsdb.path=/prometheus"
        - "--storage.tsdb.retention=24h"
        ports:
        - containerPort: 9090
          protocol: TCP
        volumeMounts:
        - mountPath: "/prometheus"
          name: data
        - mountPath: "/etc/prometheus"
          name: config-volume
        resources:
          requests:
            cpu: 100m
            memory: 100Mi
          limits:
            cpu: 500m
            memory: 2500Mi
      serviceAccountName: prometheus    
      volumes:
      - name: data
        emptyDir: {}
      - name: config-volume
        configMap:
          name: prometheus-config 
```

```shell
kubectl create -f prometheus.deploy.yml
#结果
deployment.apps/prometheus created
```

###### prometheus.svc.yml

```yaml
---
kind: Service
apiVersion: v1
metadata:
  labels:
    app: prometheus
  name: prometheus
  namespace: kube-system
spec:
  type: NodePort
  ports:
  - port: 9090
    targetPort: 9090
    nodePort: 30003
  selector:
    app: prometheus
```

```shell
kubectl create -f prometheus.svc.yml
#结果
service/prometheus created
```

#### 第二步 部署Grafana

###### grafana-deploy.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: grafana-core
  namespace: kube-system
  labels:
    app: grafana
    component: core
spec:
  replicas: 1
  selector:
      matchLabels:
        app: grafana
        component: core
  template:
    metadata:
      labels:
        app: grafana
        component: core
    spec:
      containers:
      - image: grafana/grafana:4.2.0
        name: grafana-core
        imagePullPolicy: IfNotPresent
        # env:
        resources:
          # keep request = limit to keep this container in guaranteed class
          limits:
            cpu: 100m
            memory: 100Mi
          requests:
            cpu: 100m
            memory: 100Mi
        env:
          # The following env variables set up basic auth twith the default admin user and admin password.
          - name: GF_AUTH_BASIC_ENABLED
            value: "true"
          - name: GF_AUTH_ANONYMOUS_ENABLED
            value: "false"
          # - name: GF_AUTH_ANONYMOUS_ORG_ROLE
          #   value: Admin
          # does not really work, because of template variables in exported dashboards:
          # - name: GF_DASHBOARDS_JSON_ENABLED
          #   value: "true"
        readinessProbe:
          httpGet:
            path: /login
            port: 3000
          # initialDelaySeconds: 30
          # timeoutSeconds: 1
        volumeMounts:
        - name: grafana-persistent-storage
          mountPath: /var
      volumes:
      - name: grafana-persistent-storage
        emptyDir: {}
```

```shell
kubectl create -f grafana-deploy.yaml
#结果
deployment.apps/grafana-core created
```

###### grafana-ing.yaml

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
   name: grafana
   namespace: kube-system
spec:
   rules:
   - host: k8s.grafana
     http:
       paths:
       - path: /
         backend:
          serviceName: grafana
          servicePort: 3000
```

```shell
kubectl create -f grafana-ing.yaml
#结果
ingress.extensions/grafana created
```

###### grafana-svc.yaml

```yaml
apiVersion: v1
kind: Service
metadata:
  name: grafana
  namespace: kube-system
  labels:
    app: grafana
    component: core
spec:
  type: NodePort
  ports:
    - port: 3000
  selector:
    app: grafana
    component: core
```

```shell
kubectl create -f grafana-svc.yaml
#结果
service/grafana created
```

#### 第三步 打开Grafana,配置数据源,导入显示面板

```shell
kubectl get pods -n kube-system -o wide
kubectl get svc -n kube-system -o wide
#查看grafana和prometheus的CLUSTER-IP和PORT(S)
http://192.168.31.61:31269/login    admin/admin
#配置数据源,Data Sources->Add->name=mydb,type=Prometheus,url=http://10.102.233.226:9090
#设置显示数据模板:Dashboards->import->315->mydb
```



























