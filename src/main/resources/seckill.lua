local voucherId = ARGV[1]
local userId = ARGV[2]
local id = ARGV[3]
local stockKey = "seckill:stock:"..voucherId
local orderKey = "voucher:order:"..voucherId
--判断库存是否充足
if ( tonumber(redis.call('get',stockKey)) <= 0) then
    return 1;
end
--判断是否重复下单
if redis.call('sismember',orderKey,userId) == 1 then
    return 2;
end
--减库存
redis.call('incrby',stockKey,-1)
--保存用户
redis.call('sadd',orderKey,userId);
--发布消息到消息队列
redis.call('xadd','stream.orders','*','id',id,'userId',userId,'voucherId',voucherId)
return 0;