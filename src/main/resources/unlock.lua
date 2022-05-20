if (redis.call('get',KEY[1]) == ARGV[1]) then
    --释放锁
        return redis.call('del',KEY[1])
end
return 0;