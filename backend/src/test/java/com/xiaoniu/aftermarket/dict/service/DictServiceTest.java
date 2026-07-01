package com.xiaoniu.aftermarket.dict.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiaoniu.aftermarket.dict.entity.SysDictItemEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class DictServiceTest {

    @Autowired
    private DictService dictService;

    @Test
    void listItemsByTypeCodeReturnsEnabledItems() {
        // PART_CATEGORY is ENABLED and returns enabled items only.
        List<SysDictItemEntity> items = dictService.listItemsByTypeCode("PART_CATEGORY");
        assertNotNull(items);
        assertTrue(items.size() >= 2);
        assertTrue(items.stream().anyMatch(i -> "BATTERY".equals(i.getItemCode())));
        assertTrue(items.stream().anyMatch(i -> "MOTOR".equals(i.getItemCode())));
        assertTrue(items.stream().allMatch(i -> "ENABLED".equals(i.getStatus())));
    }

    @Test
    void listItemsByTypeCodeReturnsEmptyForDisabledType() {
        // REPAIR_TYPE is DISABLED
        List<SysDictItemEntity> items = dictService.listItemsByTypeCode("REPAIR_TYPE");
        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    void listItemsByTypeCodeReturnsEmptyForNonexistentType() {
        List<SysDictItemEntity> items = dictService.listItemsByTypeCode("NONEXISTENT");
        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    void getEnabledItemReturnsExistingEnabledItem() {
        SysDictItemEntity item = dictService.getEnabledItem("PART_CATEGORY", "BATTERY");
        assertNotNull(item);
        assertEquals("BATTERY", item.getItemCode());
        assertEquals("电池", item.getItemName());
    }

    @Test
    void getEnabledItemReturnsNullForDisabledItem() {
        // BRAKE is DISABLED
        SysDictItemEntity item = dictService.getEnabledItem("PART_CATEGORY", "BRAKE");
        assertNull(item);
    }

    @Test
    void getEnabledItemReturnsNullForNonexistentItem() {
        SysDictItemEntity item = dictService.getEnabledItem("PART_CATEGORY", "NONEXISTENT");
        assertNull(item);
    }

    @Test
    void existsEnabledItemReturnsTrueForEnabled() {
        assertTrue(dictService.existsEnabledItem("PART_CATEGORY", "BATTERY"));
    }

    @Test
    void existsEnabledItemReturnsFalseForDisabled() {
        assertFalse(dictService.existsEnabledItem("PART_CATEGORY", "BRAKE"));
    }

    @Test
    void existsEnabledItemReturnsFalseForNonexistent() {
        assertFalse(dictService.existsEnabledItem("PART_CATEGORY", "NONEXISTENT"));
    }

    @Test
    void existsEnabledItemReturnsFalseForDisabledType() {
        assertFalse(dictService.existsEnabledItem("REPAIR_TYPE", "ANY"));
    }

    @Test
    void listItemsByTypeCodeReturnsEmptyForSoftDeletedType() {
        // PRIORITY is ENABLED but deleted=1
        List<SysDictItemEntity> items = dictService.listItemsByTypeCode("PRIORITY");
        assertTrue(items.isEmpty());
    }

    @Test
    void getEnabledItemReturnsNullForSoftDeletedType() {
        assertNull(dictService.getEnabledItem("PRIORITY", "ANY"));
    }
}
