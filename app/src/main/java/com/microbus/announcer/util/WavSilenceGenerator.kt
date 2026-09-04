package com.microbus.announcer.util

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavSilenceGenerator {

    private const val SAMPLE_RATE = 44100
    private const val CHANNELS = 1 // 单声道
    private const val BITS_PER_SAMPLE = 16


    /**
     * 生成静音 WAV 文件
     * @param durationMs 时长（毫秒）
     * @param filePath 输出文件完整路径
     * @return 是否生成成功
     */
    fun generateSilenceWav(durationMs: Long, filePath: String): Boolean {
        return try {

            // 创建父目录
            val file = File(filePath)
            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs() // 创建所有必要的父目录
            }

            val dataSize =
                (durationMs * SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8) / 1000).toInt()
            val totalSize = 44 + dataSize // 44字节头 + 数据


            FileOutputStream(filePath).use { fos ->
                // 写入 WAV 头
                val header = ByteBuffer.allocate(44)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .apply {
                        put("RIFF".toByteArray())          // ChunkID
                        putInt(totalSize - 8)              // ChunkSize
                        put("WAVE".toByteArray())          // Format
                        put("fmt ".toByteArray())          // Subchunk1ID
                        putInt(16)                         // Subchunk1Size (16 for PCM)
                        putShort(1)                        // AudioFormat (1 = PCM)
                        putShort(CHANNELS.toShort())       // NumChannels
                        putInt(SAMPLE_RATE)                // SampleRate
                        putInt(SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8) // ByteRate
                        putShort((CHANNELS * BITS_PER_SAMPLE / 8).toShort()) // BlockAlign
                        putShort(BITS_PER_SAMPLE.toShort()) // BitsPerSample
                        put("data".toByteArray())          // Subchunk2ID
                        putInt(dataSize)                   // Subchunk2Size
                    }
                fos.write(header.array())

                // 写入静音数据（全部填充 0）
                val buffer = ByteArray(4096)
                var written = 0
                while (written < dataSize) {
                    val chunkSize = minOf(buffer.size, dataSize - written)
                    fos.write(buffer, 0, chunkSize) // buffer 初始全为 0
                    written += chunkSize
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}