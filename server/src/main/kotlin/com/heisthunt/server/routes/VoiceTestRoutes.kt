package com.heisthunt.server.routes

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

private val voiceTestRooms = ConcurrentHashMap<String, ConcurrentHashMap<String, DefaultWebSocketSession>>()

fun Route.voiceTestRoutes() {
    route("/voice") {
        webSocket("/test/{roomId}") {
            val roomId = call.parameters["roomId"] ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "roomId required"))
                return@webSocket
            }
            val userId = call.request.queryParameters["userId"] ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "userId required"))
                return@webSocket
            }

            val room = voiceTestRooms.getOrPut(roomId) { ConcurrentHashMap() }
            room[userId] = this
            println("🎤 [VoiceTest] User $userId joined room $roomId (${room.size} users)")

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        println("📡 [VoiceTest] Relay from $userId: ${text.take(80)}")

                        // toUserId가 있으면 해당 대상에게만, 없으면 전체에 릴레이
                        val toUserId = extractToUserId(text)
                        val targets = if (toUserId != null) {
                            room.entries.filter { it.key == toUserId }
                        } else {
                            room.entries.filter { it.key != userId }
                        }

                        targets.forEach { (peerId, session) ->
                            try {
                                session.send(Frame.Text(text))
                            } catch (e: Exception) {
                                println("❌ [VoiceTest] Failed to relay to $peerId: ${e.message}")
                                room.remove(peerId)
                            }
                        }
                    }
                }
            } finally {
                room.remove(userId)
                println("🔌 [VoiceTest] User $userId left room $roomId (${room.size} remaining)")

                // 남은 멤버들에게 퇴장 알림
                val byeMsg = """{"type":"bye","userId":"$userId"}"""
                room.entries.forEach { (peerId, session) ->
                    try {
                        session.send(Frame.Text(byeMsg))
                    } catch (e: Exception) {
                        room.remove(peerId)
                    }
                }

                if (room.isEmpty()) voiceTestRooms.remove(roomId)
            }
        }
    }
}

private fun extractToUserId(json: String): String? {
    val match = Regex(""""toUserId"\s*:\s*"([^"]+)"""").find(json)
    return match?.groupValues?.get(1)
}
