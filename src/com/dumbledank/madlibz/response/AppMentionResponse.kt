package com.dumbledank.madlibz.response

import com.fasterxml.jackson.annotation.JsonProperty

data class AppMentionResponse(
    @JsonProperty("channel")
    var channel: String,
    @JsonProperty("text")
    var text: String,
    @JsonProperty("thread_ts")
    var threadTs: String? = null
)
