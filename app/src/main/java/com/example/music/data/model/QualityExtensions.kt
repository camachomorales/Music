package com.example.music.data.model

/**
 * Extensions para AudioQuality, StreamingQuality y EqualizerPreset
 */

// Extension para AudioQuality
fun AudioQuality.toKbps(): Int = this.bitrate

fun AudioQuality.toMbps(): Float = this.bitrate / 1000f

// Extension para EqualizerPreset - Valores de ecualización
fun EqualizerPreset.getEqualizerValues(): FloatArray {
    return when (this) {
        EqualizerPreset.FLAT -> floatArrayOf(0f, 0f, 0f, 0f, 0f)
        EqualizerPreset.ACOUSTIC -> floatArrayOf(5f, 4f, 3f, 2f, 4f)
        EqualizerPreset.BASS_BOOST -> floatArrayOf(7f, 5f, 0f, 0f, 0f)
        EqualizerPreset.BASS_REDUCER -> floatArrayOf(-5f, -3f, 0f, 0f, 0f)
        EqualizerPreset.CLASSICAL -> floatArrayOf(5f, 4f, 3f, 3f, 4f)
        EqualizerPreset.DANCE -> floatArrayOf(4f, 7f, 5f, 0f, 2f)
        EqualizerPreset.DEEP -> floatArrayOf(5f, 3f, 0f, 2f, 5f)
        EqualizerPreset.ELECTRONIC -> floatArrayOf(5f, 4f, 1f, 0f, 5f)
        EqualizerPreset.HIP_HOP -> floatArrayOf(7f, 4f, 0f, 2f, 4f)
        EqualizerPreset.JAZZ -> floatArrayOf(4f, 3f, 2f, 3f, 5f)
        EqualizerPreset.LATIN -> floatArrayOf(5f, 3f, 0f, 0f, 5f)
        EqualizerPreset.LOUDNESS -> floatArrayOf(6f, 4f, 0f, 0f, 6f)
        EqualizerPreset.LOUNGE -> floatArrayOf(0f, 3f, 4f, 2f, 0f)
        EqualizerPreset.PIANO -> floatArrayOf(3f, 2f, 0f, 4f, 5f)
        EqualizerPreset.POP -> floatArrayOf(0f, 3f, 5f, 4f, 0f)
        EqualizerPreset.RNB -> floatArrayOf(6f, 7f, 0f, 2f, 3f)
        EqualizerPreset.ROCK -> floatArrayOf(5f, 3f, 0f, 3f, 5f)
        EqualizerPreset.SMALL_SPEAKERS -> floatArrayOf(6f, 5f, 4f, 3f, 2f)
        EqualizerPreset.SPOKEN_WORD -> floatArrayOf(0f, 0f, 3f, 5f, 5f)
        EqualizerPreset.TREBLE_BOOST -> floatArrayOf(0f, 0f, 0f, 5f, 7f)
        EqualizerPreset.TREBLE_REDUCER -> floatArrayOf(0f, 0f, 0f, -3f, -5f)
        EqualizerPreset.VOCAL_BOOST -> floatArrayOf(0f, 0f, 5f, 6f, 5f)
    }
}

// Extension para obtener descripción del preset
fun EqualizerPreset.getDescription(): String {
    return when (this) {
        EqualizerPreset.FLAT -> "No equalization applied"
        EqualizerPreset.ACOUSTIC -> "Optimized for acoustic music"
        EqualizerPreset.BASS_BOOST -> "Enhanced bass frequencies"
        EqualizerPreset.BASS_REDUCER -> "Reduced bass frequencies"
        EqualizerPreset.CLASSICAL -> "Optimized for classical music"
        EqualizerPreset.DANCE -> "Optimized for dance music"
        EqualizerPreset.DEEP -> "Deep, rich sound"
        EqualizerPreset.ELECTRONIC -> "Optimized for electronic music"
        EqualizerPreset.HIP_HOP -> "Optimized for hip-hop music"
        EqualizerPreset.JAZZ -> "Optimized for jazz music"
        EqualizerPreset.LATIN -> "Optimized for latin music"
        EqualizerPreset.LOUDNESS -> "Increased overall volume"
        EqualizerPreset.LOUNGE -> "Smooth, relaxed sound"
        EqualizerPreset.PIANO -> "Optimized for piano"
        EqualizerPreset.POP -> "Optimized for pop music"
        EqualizerPreset.RNB -> "Optimized for R&B music"
        EqualizerPreset.ROCK -> "Optimized for rock music"
        EqualizerPreset.SMALL_SPEAKERS -> "Optimized for small speakers"
        EqualizerPreset.SPOKEN_WORD -> "Optimized for podcasts and audiobooks"
        EqualizerPreset.TREBLE_BOOST -> "Enhanced treble frequencies"
        EqualizerPreset.TREBLE_REDUCER -> "Reduced treble frequencies"
        EqualizerPreset.VOCAL_BOOST -> "Enhanced vocal clarity"
    }
}