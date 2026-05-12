package com.xiaoniu.aftermarket.part.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaoniu.aftermarket.part.entity.PartBarcodeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PartBarcodeMapper extends BaseMapper<PartBarcodeEntity> {

    @Select("""
            SELECT * FROM part_barcode
            WHERE store_id = #{storeId} AND barcode = #{barcode} AND deleted = 0
            LIMIT 1
            """)
    PartBarcodeEntity selectByStoreIdAndBarcode(@Param("storeId") Long storeId, @Param("barcode") String barcode);
}
