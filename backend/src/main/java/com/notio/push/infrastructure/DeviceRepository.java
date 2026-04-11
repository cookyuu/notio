package com.notio.push.infrastructure;

import com.notio.push.domain.Device;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByToken(String token);
}

