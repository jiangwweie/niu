package com.xiaoniu.aftermarket.workorder.dto;

public interface DraftSnapshotWritable {

    Long getStoreId();

    Long getOperatorId();

    Long getCustomerId();

    void setCustomerId(Long customerId);

    Long getVehicleId();

    void setVehicleId(Long vehicleId);

    String getCustomerNameSnapshot();

    String getCustomerPhoneSnapshot();

    String getVehicleModelSnapshot();

    String getFrameNoSnapshot();

    String getBatteryNoSnapshot();

    void setVehicleModelSnapshot(String vehicleModelSnapshot);

    void setFrameNoSnapshot(String frameNoSnapshot);

    void setBatteryNoSnapshot(String batteryNoSnapshot);

    void setCustomerNameSnapshot(String customerNameSnapshot);

    void setCustomerPhoneSnapshot(String customerPhoneSnapshot);
}
