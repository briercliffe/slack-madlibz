package com.dumbledank.madlibz.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChannelMessageEvent(
    @JsonProperty("channel")
    var channel: String,
    @JsonProperty("channel_type")
    var channelType: String,
    @JsonProperty("event_ts")
    var eventTs: String,
    @JsonProperty("text")
    var text: String?,
    @JsonProperty("ts")
    var ts: String,
    @JsonProperty("thread_ts")
    var threadTs: String?,
    @JsonProperty("type")
    var type: String,
    @JsonProperty("subtype")
    var subtype: String?,
    @JsonProperty("user")
    var user: String?
)
