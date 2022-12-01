package com.botdetectorantispam.enums;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageState {
    ALLOWED,
    IGNORED
}