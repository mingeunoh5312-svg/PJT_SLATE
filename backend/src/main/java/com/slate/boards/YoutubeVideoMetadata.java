package com.slate.boards;

public record YoutubeVideoMetadata(
        String videoId,
        String embedUrl,
        String youtubeUrl,
        String originalUrl,
        String title,
        String channelTitle,
        String thumbnailUrl,
        Integer durationSeconds
) {
}
