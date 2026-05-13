package com.notio.channel.service;

import com.notio.channel.ChannelProviderRegistry;
import com.notio.channel.domain.ChannelType;
import com.notio.channel.domain.NotificationChannel;
import com.notio.channel.provider.ChannelDeliveryResult;
import com.notio.channel.provider.ChannelMessage;
import com.notio.channel.provider.ChannelValidationResult;
import com.notio.channel.provider.NotificationChannelProvider;
import com.notio.channel.repository.NotificationChannelRepository;
import com.notio.common.exception.ErrorCode;
import com.notio.common.exception.NotioException;
import com.notio.connection.security.CredentialEncryptionService;
import com.notio.notification.domain.NotificationPriority;
import com.notio.notification.domain.NotificationSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationChannelService {

    private final NotificationChannelRepository channelRepository;
    private final ChannelProviderRegistry providerRegistry;
    private final CredentialEncryptionService encryptionService;

    public List<NotificationChannel> findAll(Long userId) {
        return channelRepository.findAllByUserId(userId);
    }

    public NotificationChannel findById(Long userId, Long channelId) {
        NotificationChannel channel = channelRepository.findById(channelId)
            .orElseThrow(() -> new NotioException(ErrorCode.CHANNEL_NOT_FOUND));
        if (!channel.getUserId().equals(userId)) {
            throw new NotioException(ErrorCode.FORBIDDEN);
        }
        return channel;
    }

    @Transactional
    public NotificationChannel create(
        Long userId,
        ChannelType channelType,
        String displayName,
        String credentialPlaintext,
        String targetIdentifier
    ) {
        NotificationChannelProvider provider = providerRegistry.get(channelType);
        ChannelValidationResult validation = provider.validate(credentialPlaintext, targetIdentifier);
        if (!validation.isValid()) {
            throw new NotioException(ErrorCode.CHANNEL_CREDENTIAL_INVALID, validation.errorMessage());
        }

        String encrypted = encryptionService.encrypt(credentialPlaintext);
        NotificationChannel channel = NotificationChannel.builder()
            .userId(userId)
            .channelType(channelType)
            .displayName(displayName)
            .credentialEncrypted(encrypted)
            .targetIdentifier(targetIdentifier)
            .build();

        return channelRepository.save(channel);
    }

    @Transactional
    public NotificationChannel update(
        Long userId,
        Long channelId,
        String displayName,
        String credentialPlaintext,
        String targetIdentifier
    ) {
        NotificationChannel channel = findById(userId, channelId);

        if (credentialPlaintext != null && !credentialPlaintext.isBlank()) {
            NotificationChannelProvider provider = providerRegistry.get(channel.getChannelType());
            ChannelValidationResult validation = provider.validate(credentialPlaintext, targetIdentifier);
            if (!validation.isValid()) {
                throw new NotioException(ErrorCode.CHANNEL_CREDENTIAL_INVALID, validation.errorMessage());
            }
            String encrypted = encryptionService.encrypt(credentialPlaintext);
            channel.updateCredential(encrypted, targetIdentifier);
        }

        if (displayName != null && !displayName.isBlank()) {
            channel.updateDisplayName(displayName);
        }

        return channelRepository.save(channel);
    }

    @Transactional
    public void delete(Long userId, Long channelId) {
        NotificationChannel channel = findById(userId, channelId);
        channelRepository.delete(channel);
    }

    @Transactional
    public NotificationChannel pause(Long userId, Long channelId) {
        NotificationChannel channel = findById(userId, channelId);
        channel.pause();
        return channelRepository.save(channel);
    }

    @Transactional
    public NotificationChannel resume(Long userId, Long channelId) {
        NotificationChannel channel = findById(userId, channelId);
        channel.resume();
        return channelRepository.save(channel);
    }

    public ChannelDeliveryResult test(Long userId, Long channelId) {
        NotificationChannel channel = findById(userId, channelId);
        NotificationChannelProvider provider = providerRegistry.get(channel.getChannelType());
        ChannelMessage testMessage = new ChannelMessage(
            0L,
            "[테스트] Notio 채널 연결 확인",
            "이 메시지는 Notio 채널 연결 테스트를 위한 메시지입니다.",
            NotificationSource.CLAUDE,
            NotificationPriority.LOW,
            null,
            Instant.now()
        );
        return provider.deliver(channel, testMessage);
    }
}
