# 【性能测试】验证服务在aws上的性能表现

## 测试目的

因为我们在[验证不同数据量级下的性能表现](./performance-local-with-data-size.md)已经得出数据量级和当前系统性能瓶颈无关的结论，因此在aws上只需要构造不同并发进行测试即可，无需再构造数据（aws上构造数据相对比较困难，涉及数据库访问权限的配置，目前还没有搞定）

## 测试环境

wrk 4.2.0
MacOS M2 2022 24G Sequoia 15.2
aws-rds(mysql 8.4.3), vCPU:2, RAM:1GB
aws-elastic cache(Valkey 8.0)
EKS: 3 pods, limit cpu 500m, up to 1000, limit memroy 500M, up to 1GB

## 测试方法

通过[performance_test_transaction.py](../../scripts/test/performance_test_transaction.sh)，构造几个档位的并发验证性能变化，对比不同量级间的差异

## 测试结论

最佳表现是300并发下，QPS=20，延迟=500ms。显然这个数据并不是很理想，需要进一步通过结合Prometheus分析链路延迟，查找相关瓶颈

## 测试数据

见[local](./aws/)
