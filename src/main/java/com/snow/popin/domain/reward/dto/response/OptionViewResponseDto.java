package com.snow.popin.domain.reward.dto.response;


import lombok.*;

    @Data
    @AllArgsConstructor @NoArgsConstructor @Builder
    public class OptionViewResponseDto {
        private Long id;
        private String name;
        private int total;
        private int issued;
        private int remaining;
    }
