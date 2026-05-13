package com.xiaoniu.aftermarket.official.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoniu.aftermarket.official.entity.OfficialAfterSalesEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OfficialAfterSalesMapper extends BaseMapper<OfficialAfterSalesEntity> {

    @Results(id = "officialAfterSalesResultMap", value = {
            @Result(column = "is_official_after_sales", property = "officialAfterSales")
    })
    @Select("""
            SELECT * FROM official_after_sales
            WHERE work_order_id = #{workOrderId} AND deleted = 0
            LIMIT 1
            """)
    OfficialAfterSalesEntity selectByWorkOrderId(@Param("workOrderId") Long workOrderId);

    @Select("""
            SELECT * FROM official_after_sales
            WHERE work_order_id = #{workOrderId} AND deleted = 0
            LIMIT 1 FOR UPDATE
            """)
    @ResultMap("officialAfterSalesResultMap")
    OfficialAfterSalesEntity selectByWorkOrderIdForUpdate(@Param("workOrderId") Long workOrderId);

    @Select("""
            SELECT * FROM official_after_sales
            WHERE store_id = #{storeId}
              AND official_order_no = #{officialOrderNo}
              AND deleted = 0
            LIMIT 1
            """)
    @ResultMap("officialAfterSalesResultMap")
    OfficialAfterSalesEntity selectByStoreIdAndOfficialOrderNo(@Param("storeId") Long storeId,
                                                               @Param("officialOrderNo") String officialOrderNo);
}
