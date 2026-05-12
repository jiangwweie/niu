package com.xiaoniu.aftermarket.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoniu.aftermarket.customer.entity.CustomerEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<CustomerEntity> {
}
