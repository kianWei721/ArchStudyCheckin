package com.archstudy.checkin.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentUserResponse {

    private Long userId;
    private String username;
    private String email;
}
