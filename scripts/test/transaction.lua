local account_ids = {}

for i = 60088, 100088 do
    table.insert(account_ids, tostring(i))
end
local random = math.random

-- 设置默认场景
local scene = "deposit"

-- 初始化方法，用于获取命令行参数
init = function(args)
    if #args > 0 then
        scene = args[1] or 'deposit'
    end
end

request = function()
    local account_id = account_ids[random(#account_ids)]
    local request_body

    if scene == "deposit" then
        request_body = string.format('{"type":"DEPOSIT","sourceAccountId":"%s","amount":%d}', account_id, random(10, 1000))
    elseif scene == "withdraw" then
        request_body = string.format('{"type":"WITHDRAW","sourceAccountId":"%s","amount":%d}', account_id, random(10, 1000))
    elseif scene == "transfer" then
        request_body = string.format('{"type":"TRANSFER","sourceAccountId":"%s","destinationAccountId":"%s","amount":%d}',
            account_id, 
            account_ids[random(#account_ids)], 
            random(1, 10))
    else
        error("Unsupported scene: " .. scene)
    end

    return wrk.format("POST", "/api/v1/transactions", {
        ["Content-Type"] = "application/json",
        ["Content-Length"] = string.len(request_body)
    }, request_body)
end