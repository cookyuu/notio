package com.notio.channel.service;

import com.notio.channel.domain.ChannelType;
import com.notio.channel.dto.DeliveryFeedItem;
import com.notio.channel.repository.DeliveryFeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryFeedService {

    private final DeliveryFeedRepository deliveryFeedRepository;

    public Page<DeliveryFeedItem> getFeed(Long userId, ChannelType channelType, Pageable pageable) {
        return deliveryFeedRepository.findFeed(userId, channelType, pageable);
    }
}
