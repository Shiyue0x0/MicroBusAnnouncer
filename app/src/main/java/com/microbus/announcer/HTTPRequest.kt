package com.microbus.announcer

data class ParsedRequest(
    val name: String,             // 请求名称（第一行）
    val method: String,           // GET / POST
    val url: String,
    val headers: Map<String, String>,
    val body: String?
)

class HTTPRequest {

    fun parseRequest(input: String): ParsedRequest {
        val trimmed = input.trim()
        val lines = trimmed.lines()

        if (lines.size < 2) {
            throw IllegalArgumentException("至少需要两行：第一行请求名称，第二行方法和URL")
        }

        // 1. 第一行：请求名称
        val name = lines[0].trim()
        if (name.isEmpty()) {
            throw IllegalArgumentException("请求名称不能为空")
        }

        // 2. 第二行：方法 + URL
        val secondLine = lines[1].trim()
        val parts = secondLine.split(Regex("\\s+"), limit = 2)
        if (parts.size < 2) {
            throw IllegalArgumentException("第二行格式应为：GET/POST 空格 URL")
        }

        val method = parts[0].uppercase()
        if (method != "GET" && method != "POST") {
            throw IllegalArgumentException("只支持 GET 或 POST")
        }
        val url = parts[1].trim()

        // 3. 之后：headers + body（空行分隔）
        val rest = lines.drop(2)
        val blankIndex = rest.indexOfFirst { it.isBlank() }

        val headerLines: List<String>
        val bodyLines: List<String>
        if (blankIndex == -1) {
            if (method == "GET") {
                headerLines = rest.filter { it.contains(":") }
                bodyLines = emptyList()
            } else {
                headerLines = emptyList()
                bodyLines = rest
            }
        } else {
            headerLines = rest.take(blankIndex)
            bodyLines = rest.drop(blankIndex + 1)
        }

        val headers = headerLines
            .filter { it.isNotBlank() && it.contains(":") }
            .associate { line ->
                val idx = line.indexOf(':')
                line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }

        val body = if (bodyLines.isEmpty()) null else bodyLines.joinToString("\n")

        return ParsedRequest(name, method, url, headers, body)
    }
}