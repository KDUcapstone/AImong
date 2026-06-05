package com.kduniv.aimong.feature.quiz.data.gson

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.kduniv.aimong.feature.quiz.data.model.RewardResponse

/**
 * v2.5: `attemptId`가 UUID 문자열 또는 숫자로 올 수 있음.
 */
class AttemptIdStringAdapter : TypeAdapter<String?>() {
    override fun write(out: JsonWriter, value: String?) {
        if (value == null) out.nullValue() else out.value(value)
    }

    override fun read(reader: JsonReader): String? {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            JsonToken.STRING -> reader.nextString()
            JsonToken.NUMBER -> reader.nextLong().toString()
            JsonToken.BOOLEAN -> reader.nextBoolean().toString()
            else -> {
                reader.skipValue()
                null
            }
        }
    }
}

/**
 * v2.10/11 제출·리포트: `rewards` 객체 `{ gear, exp, fragments }` 또는 레거시 배열.
 * 일반 모드 통과 시에만 gear가 내려오며, 복습·실패는 gear 없음(BE).
 */
class SubmitRewardsListAdapter : TypeAdapter<List<RewardResponse>?>() {

    private val gson = Gson()

    override fun write(out: JsonWriter, value: List<RewardResponse>?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.beginArray()
        for (r in value) {
            gson.toJson(r, RewardResponse::class.java, out)
        }
        out.endArray()
    }

    override fun read(reader: JsonReader): List<RewardResponse>? {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            JsonToken.BEGIN_ARRAY -> {
                val list = ArrayList<RewardResponse>()
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(gson.fromJson(reader, RewardResponse::class.java))
                }
                reader.endArray()
                list
            }
            JsonToken.BEGIN_OBJECT -> {
                val obj = JsonParser.parseReader(reader).asJsonObject
                parseRewardsObject(obj)
            }
            else -> {
                reader.skipValue()
                emptyList()
            }
        }
    }

    companion object {
        internal fun parseRewardsObject(obj: com.google.gson.JsonObject): List<RewardResponse> {
            val out = ArrayList<RewardResponse>()
            if (obj.has("gear") && !obj.get("gear").isJsonNull) {
                val g = obj.get("gear").asInt
                if (g > 0) out.add(RewardResponse(type = "GEAR", ticketType = null, count = g, reason = null))
            }
            if (obj.has("coin") && !obj.get("coin").isJsonNull) {
                val c = obj.get("coin").asInt
                if (c > 0) out.add(RewardResponse(type = "COIN", ticketType = null, count = c, reason = null))
            }
            if (obj.has("exp") && !obj.get("exp").isJsonNull) {
                val e = obj.get("exp").asInt
                if (e > 0) out.add(RewardResponse(type = "EXP", ticketType = null, count = e, reason = null))
            }
            if (obj.has("fragments") && obj.get("fragments").isJsonArray) {
                val arr = obj.getAsJsonArray("fragments")
                for (el in arr) {
                    when {
                        el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> {
                            val n = el.asInt
                            if (n > 0) {
                                out.add(RewardResponse(type = "PET_FRAGMENT", ticketType = null, count = n, reason = null))
                            }
                        }
                        el.isJsonObject -> {
                            val n = el.asJsonObject.get("count")?.takeIf { !it.isJsonNull }?.asInt ?: 1
                            out.add(RewardResponse(type = "PET_FRAGMENT", ticketType = null, count = n.coerceAtLeast(1), reason = null))
                        }
                    }
                }
            }
            return out
        }
    }
}
