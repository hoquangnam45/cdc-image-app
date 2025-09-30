package com.hoquangnam45.cdc.image.app.event.config;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;

@Configuration
public class PubsubConfig {
    @Bean
    public MessageChannel uploadedImageChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter uploadedImageChannelAdapter(PubSubTemplate pubSubTemplate, MessageChannel uploadedImageChannel, @Value("${app.subscription.upload-image}") String subscriptionName) {
        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(uploadedImageChannel);
        adapter.setPayloadType(String.class);
        adapter.setAckMode(AckMode.MANUAL);

        return adapter;
    }
}
