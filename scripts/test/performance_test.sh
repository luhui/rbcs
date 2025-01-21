#!/bin/bash

# 创建账号性能测试
echo "测试创建账号..."
ab -n 1000 -c 100 -p scripts/test/create_account.json -T application/json -g scripts/test/create_account_results.html http://localhost:8080/api/accounts

# 激活账号性能测试
echo "测试激活账号..."
ab -n 1000 -c 100 -p scripts/test/activate_account.json -T application/json -g scripts/test/activate_account_results.html http://localhost:8080/api/accounts/1/activate

# 存款交易性能测试
echo "测试存款交易..."
ab -n 1000 -c 100 -p scripts/test/deposit.json -T application/json -g scripts/test/deposit_results.html http://localhost:8080/api/transactions

# 取款交易性能测试
echo "测试取款交易..."
ab -n 1000 -c 100 -p scripts/test/withdraw.json -T application/json -g scripts/test/withdraw_results.html http://localhost:8080/api/transactions

# 转账交易性能测试
echo "测试转账交易..."
ab -n 1000 -c 100 -p scripts/test/transfer.json -T application/json -g scripts/test/transfer_results.html http://localhost:8080/api/transactions