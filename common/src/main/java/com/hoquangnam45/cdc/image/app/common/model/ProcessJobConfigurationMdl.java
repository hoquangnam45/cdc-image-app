package com.hoquangnam45.cdc.image.app.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProcessJobConfigurationMdl {
        private UUID id;
        private Integer width;
        private Integer height;
        private BigDecimal scale;
        private Boolean keepRatio;
        private Integer quality;
        private String description;
        private String outputFileType;
        private String fileType;
}
