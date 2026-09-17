package com.example.kaoyanfocus.data

import org.junit.Assert.assertEquals
import org.junit.Test

class QwenEndpointTest {
    @Test fun normalizesDashScopeRootAndCompatibleEndpoint() {
        assertEquals(
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            qwenChatCompletionsUrl("https://dashscope.aliyuncs.com")
        )
        assertEquals(
            "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            qwenChatCompletionsUrl("https://dashscope.aliyuncs.com/compatible-mode/v1/")
        )
    }

    @Test fun doesNotDuplicateFullChatEndpoint() {
        assertEquals(
            "https://example.com/v1/chat/completions",
            qwenChatCompletionsUrl("https://example.com/v1/chat/completions")
        )
    }
}
