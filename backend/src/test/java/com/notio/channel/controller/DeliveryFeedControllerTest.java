package com.notio.channel.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notio.channel.domain.ChannelType;
import com.notio.channel.domain.DeliveryStatus;
import com.notio.channel.dto.DeliveryFeedItem;
import com.notio.channel.service.DeliveryFeedService;
import com.notio.common.exception.NotioException;
import com.notio.common.response.ApiResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class DeliveryFeedControllerTest {

    @Mock
    private DeliveryFeedService deliveryFeedService;

    private DeliveryFeedController controller;

    @BeforeEach
    void setUp() {
        controller = new DeliveryFeedController(deliveryFeedService);
    }

    @Test
    void getDeliveryFeedReturnsPagedFeedItems() {
        DeliveryFeedItem item = new DeliveryFeedItem(
            1L, 10L, "PR merged", 1L, ChannelType.SLACK,
            "dev-channel", "Summary text", Instant.now(), DeliveryStatus.SUCCESS, "ext-id"
        );
        when(deliveryFeedService.getFeed(any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        ApiResponse<Page<DeliveryFeedItem>> response = controller.getDeliveryFeed(
            0, 20, null, new UsernamePasswordAuthenticationToken("1", null)
        );

        assertThat(response.success()).isTrue();
        assertThat(response.data().getContent()).hasSize(1);
        assertThat(response.data().getContent().getFirst().notificationTitle()).isEqualTo("PR merged");
    }

    @Test
    void getDeliveryFeedPassesUserIdFromAuthentication() {
        when(deliveryFeedService.getFeed(any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of()));

        controller.getDeliveryFeed(0, 20, null, new UsernamePasswordAuthenticationToken("42", null));

        verify(deliveryFeedService).getFeed(eq(42L), any(), any());
    }

    @Test
    void getDeliveryFeedPassesChannelTypeFilter() {
        when(deliveryFeedService.getFeed(any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of()));

        controller.getDeliveryFeed(0, 20, ChannelType.TELEGRAM,
            new UsernamePasswordAuthenticationToken("1", null));

        verify(deliveryFeedService).getFeed(eq(1L), eq(ChannelType.TELEGRAM), any());
    }

    @Test
    void getDeliveryFeedCapsPageSizeAtFifty() {
        when(deliveryFeedService.getFeed(any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of()));

        controller.getDeliveryFeed(0, 200, null, new UsernamePasswordAuthenticationToken("1", null));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(deliveryFeedService).getFeed(any(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void getDeliveryFeedSortsByDeliveredAtDescending() {
        when(deliveryFeedService.getFeed(any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of()));

        controller.getDeliveryFeed(0, 20, null, new UsernamePasswordAuthenticationToken("1", null));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(deliveryFeedService).getFeed(any(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("deliveredAt")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("deliveredAt").isDescending()).isTrue();
    }

    @Test
    void getDeliveryFeedThrowsUnauthorizedWhenAuthenticationIsNull() {
        assertThatThrownBy(() -> controller.getDeliveryFeed(0, 20, null, null))
            .isInstanceOf(NotioException.class);
    }

    @Test
    void getDeliveryFeedThrowsUnauthorizedWhenPrincipalIsNotNumeric() {
        assertThatThrownBy(() -> controller.getDeliveryFeed(0, 20, null,
            new UsernamePasswordAuthenticationToken("not-a-number", null)))
            .isInstanceOf(NotioException.class);
    }

    @Test
    void getDeliveryFeedReturnsPageWithCorrectPageNumber() {
        when(deliveryFeedService.getFeed(any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 20), 100));

        ApiResponse<Page<DeliveryFeedItem>> response = controller.getDeliveryFeed(
            2, 20, null, new UsernamePasswordAuthenticationToken("1", null)
        );

        assertThat(response.data().getNumber()).isEqualTo(2);
        assertThat(response.data().getTotalElements()).isEqualTo(100);
    }
}
