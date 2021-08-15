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

## 二、

### 1. deployment

```shell
#准备工作
docker pull nginx:1.18.0
docker pull nginx:1.20.0
#第一步 导出yaml文件
kubectl create deployment nginx-deploy --image=nginx:1.18.0 --dry-run -o yaml > deploy01.yaml
#第二步 使用yaml部署应用
kubectl apply -f deploy01.yaml
#第三步 对外发布(暴露对外端口号)
kubectl expose deployment nginx-deploy --port=80 --target-port=80 --type=NodePort --name=nginx-svc -o yaml > deploy02.yaml
kubectl apply -f deploy02.yaml

#查看部署状态  想看更多后面加 -o wide
kubectl get deploy
kubectl get pods,svc
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

### 2. StatefulSet

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
kubectl delete statefulset [set名字]
kubectl delete statefulset --all
```

### 3. DaemonSet

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
kubectl exec -it nginx-daemonset-9cs94 bash
ls /tmp/log
exit
kubectl get daemonset
```

### 4. Job

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

### 5. CronJob

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
            - date; echo Hello from the Kubernetes cluster
          restartPolicy: OnFailure
```

```shell
kubectl get cronjob
kubectl logs -f busybox-cronjob-1628949240-zz29x
```





