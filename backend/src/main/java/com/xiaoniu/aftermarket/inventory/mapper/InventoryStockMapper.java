package com.xiaoniu.aftermarket.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InventoryStockMapper extends BaseMapper<InventoryStockEntity> {

    @Select("""
            SELECT * FROM inventory_stock
            WHERE store_id = #{storeId} AND part_id = #{partId} AND deleted = 0
            LIMIT 1
            """)
    InventoryStockEntity selectByStoreIdAndPartId(@Param("storeId") Long storeId, @Param("partId") Long partId);

    // FOR UPDATE 行锁：防止并发操作导致库存超卖或数据不一致
    @Select("""
            SELECT * FROM inventory_stock
            WHERE store_id = #{storeId} AND part_id = #{partId} AND deleted = 0
            LIMIT 1 FOR UPDATE
            """)
    InventoryStockEntity selectByStoreIdAndPartIdForUpdate(@Param("storeId") Long storeId, @Param("partId") Long partId);
}
