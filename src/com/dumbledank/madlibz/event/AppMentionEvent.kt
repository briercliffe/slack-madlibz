package com.dumbledank.madlibz.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class AppMentionEvent(
    @JsonProperty("channel")
    var channel: String,
    @JsonProperty("client_msg_id")
    var clientMsgId: String,
    @JsonProperty("event_ts")
    var eventTs: String,
    @JsonProperty("text")
    var text: String,
    @JsonProperty("ts")
    var ts: String,
    @JsonProperty("type")
    var type: String,
    @JsonProperty("user")
    var user: String
)
