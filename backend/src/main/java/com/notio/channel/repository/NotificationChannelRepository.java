package com.notio.channel.repository;

import com.notio.channel.domain.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, Long> {

    List<NotificationChannel> findAllByUserId(Long userId);
}
