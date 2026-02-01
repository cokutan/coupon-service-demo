package com.onepin.couponservice.exception;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
  private int status;
  private String message;
  private String code;
  @Builder.Default private LocalDateTime timestamp = LocalDateTime.now();
}
