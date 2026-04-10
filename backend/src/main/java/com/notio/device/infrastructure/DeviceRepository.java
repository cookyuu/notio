package com.notio.device.infrastructure;

import com.notio.device.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    @Query("SELECT d FROM Device d WHERE d.fcmToken = :fcmToken AND d.deletedAt IS NULL")
    Optional<Device> findByFcmToken(@Param("fcmToken") String fcmToken);

    @Query("SELECT d FROM Device d WHERE d.deviceId = :deviceId AND d.deletedAt IS NULL")
    Optional<Device> findByDeviceId(@Param("deviceId") String deviceId);

    @Query("SELECT d FROM Device d WHERE d.userId = :userId AND d.active = true AND d.deletedAt IS NULL")
    List<Device> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT d FROM Device d WHERE d.active = true AND d.deletedAt IS NULL")
    List<Device> findAllActive();
}
