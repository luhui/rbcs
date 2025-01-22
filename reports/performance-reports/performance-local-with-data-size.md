# 【性能测试】验证不同数据量级下的性能表现

## 测试目的

测试系统在不同数据量级下的性能表现，以评估未来是否需要进行分库分表的演进，以确定演进的时机

## 测试环境

wrk 4.2.0
MacOS M2 2022 24G Sequoia 15.2
mysql 5.7.30
redis 7.4

## 测试方法

使用[data_generator.py](../../scripts/test/data_generator.py)构造不同量级的数据
通过[performance_test_transaction.py](../../scripts/test/performance_test_transaction.sh)，构造几个档位的并发验证性能变化，对比不同量级间的差异

## 测试结论

在1000w级别以内，数据量级带来的差异并不影响系统最终的性能结果，说明瓶颈不在数据量级上，暂不用考虑分库分表。

这个结论和业内共识的2000w以内数据对查询性能影响不大的经验是匹配的。超过1000w后可以再考虑相关的优化。

## 测试数据

见[local](./local/)
