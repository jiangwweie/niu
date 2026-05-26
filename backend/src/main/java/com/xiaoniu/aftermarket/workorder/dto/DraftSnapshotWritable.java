package com.xiaoniu.aftermarket.workorder.dto;

public interface DraftSnapshotWritable {

    void setCustomerId(Long customerId);

    void setVehicleModelSnapshot(String vehicleModelSnapshot);

    void setFrameNoSnapshot(String frameNoSnapshot);

    void setBatteryNoSnapshot(String batteryNoSnapshot);

    void setCustomerNameSnapshot(String customerNameSnapshot);

    void setCustomerPhoneSnapshot(String customerPhoneSnapshot);
}
