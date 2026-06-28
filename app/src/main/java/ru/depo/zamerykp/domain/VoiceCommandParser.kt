package ru.depo.zamerykp.domain

sealed interface VoiceCommand {
    data object StartMeasurementFlow : VoiceCommand
    data class StartMeasurementWithSide(val wheelPairNumber: Int? = null, val side: WheelSide) : VoiceCommand
    data class SelectSide(val wheelPairNumber: Int? = null, val side: WheelSide) : VoiceCommand
    data class SetValue(val field: MeasurementField, val value: Double) : VoiceCommand
    data class FillSide(
        val wheelPairNumber: Int? = null,
        val side: WheelSide,
        val flangeThickness: Double,
        val flangeWear: Double,
        val flangeSteepness: Double,
        val bandageThickness: Double,
    ) : VoiceCommand
    data class FillCurrentSide(
        val flangeThickness: Double,
        val flangeWear: Double,
        val flangeSteepness: Double,
        val bandageThickness: Double,
    ) : VoiceCommand
    data object Next : VoiceCommand
    data object FinishMeasurements : VoiceCommand
    data object SendEmail : VoiceCommand
    data object SendBluetooth : VoiceCommand
    data object SaveFile : VoiceCommand
    data class Unknown(val raw: String) : VoiceCommand
}

enum class MeasurementField(val label: String) {
    FLANGE_THICKNESS("гребень"),
    FLANGE_WEAR("прокат"),
    FLANGE_STEEPNESS("крутизна"),
    BANDAGE_THICKNESS("бандаж")
}

data class VoiceParseResult(
    val command: VoiceCommand,
    val normalizedText: String,
    val needsConfirmation: Boolean,
)

class VoiceCommandParser {
    private val startWords = setOf(
        "замер",
        "замеры",
        "новый",
        "новая",
        "новой",
        "начать",
        "начни",
        "старт",
    )
    private val ordinals = mapOf(
        "первая" to 1, "первый" to 1, "первую" to 1, "один" to 1, "одна" to 1, "одно" to 1,
        "вторая" to 2, "второй" to 2, "вторую" to 2, "два" to 2, "две" to 2,
        "третья" to 3, "третий" to 3, "третью" to 3, "три" to 3,
        "четвертая" to 4, "четвёртая" to 4, "четвертый" to 4, "четвёртый" to 4, "четыре" to 4,
        "пятая" to 5, "пятый" to 5, "пять" to 5,
        "шестая" to 6, "шестой" to 6, "шесть" to 6,
        "седьмая" to 7, "седьмой" to 7, "семь" to 7,
        "восьмая" to 8, "восьмой" to 8, "восемь" to 8,
        "девятая" to 9, "девятый" to 9, "девять" to 9,
        "десятая" to 10, "десятый" to 10, "десять" to 10,
        "одиннадцатая" to 11, "одиннадцатый" to 11,
        "двенадцатая" to 12, "двенадцатый" to 12,
    )
    private val units = mapOf(
        "ноль" to 0,
        "один" to 1, "одна" to 1, "одно" to 1,
        "два" to 2, "две" to 2,
        "три" to 3,
        "четыре" to 4,
        "пять" to 5,
        "шесть" to 6,
        "семь" to 7,
        "восемь" to 8,
        "девять" to 9,
    )
    private val teens = mapOf(
        "десять" to 10,
        "одиннадцать" to 11,
        "двенадцать" to 12,
        "тринадцать" to 13,
        "четырнадцать" to 14,
        "пятнадцать" to 15,
        "шестнадцать" to 16,
        "семнадцать" to 17,
        "восемнадцать" to 18,
        "девятнадцать" to 19,
    )
    private val tens = mapOf(
        "двадцать" to 20,
        "тридцать" to 30,
        "сорок" to 40,
        "пятьдесят" to 50,
        "шестьдесят" to 60,
        "семьдесят" to 70,
        "восемьдесят" to 80,
        "девяносто" to 90,
    )

    fun parse(rawText: String, confidence: Float = 1f): VoiceParseResult {
        val text = rawText.lowercase()
            .replace('ё', 'е')
            .replace(",", ".")
            .trim()
            .replace(Regex("\\s+"), " ")
        val normalized = normalizeSideShortcuts(text)
        val command = parseCommand(normalized)
        return VoiceParseResult(
            command = command,
            normalizedText = normalized,
            needsConfirmation = confidence < 0.65f || command is VoiceCommand.Unknown,
        )
    }

    private fun normalizeSideShortcuts(text: String): String {
        return text
            .replace(Regex("(?<!\\S)(\\d+)\\s*(?:п[эе]|пэ|пе)\\b"), "$1 правая")
            .replace(Regex("(?<!\\S)(\\d+)\\s*(?:э[лль]|эл|ел)\\b"), "$1 левая")
            .replace(Regex("\\bп[эе]\\b"), "правая")
            .replace(Regex("\\bэ[лль]\\b"), "левая")
    }

    private fun parseCommand(text: String): VoiceCommand {
        if (isStartMeasurementFlow(text)) {
            resolveSide(text)?.let { side ->
                return VoiceCommand.StartMeasurementWithSide(
                    wheelPairNumber = extractPairNumber(text),
                    side = side,
                )
            }
            return VoiceCommand.StartMeasurementFlow
        }
        if (text in listOf("следующая", "следующий", "дальше", "следующая колесная пара")) {
            return VoiceCommand.Next
        }
        if (text.contains("замеры окончены") || text.contains("закончить замер")) {
            return VoiceCommand.FinishMeasurements
        }
        if (text.contains("почт")) return VoiceCommand.SendEmail
        if (text.contains("bluetooth") || text.contains("блютуз") || text.contains("блютус")) return VoiceCommand.SendBluetooth
        if (text.contains("сохрани") || text.contains("сохранить")) return VoiceCommand.SaveFile

        val side = resolveSide(text)
        if (side != null) {
            val pairNumber = extractPairNumber(text)
            val numbers = normalizeSideValues(extractNumbers(afterSideMarker(text)))
            if (numbers.size >= 4) {
                return VoiceCommand.FillSide(
                    wheelPairNumber = pairNumber,
                    side = side,
                    flangeThickness = numbers[0],
                    flangeWear = numbers[1],
                    flangeSteepness = numbers[2],
                    bandageThickness = numbers[3],
                )
            }
            if (isSimpleSideSelection(text)) {
                return VoiceCommand.StartMeasurementWithSide(
                    wheelPairNumber = pairNumber,
                    side = side,
                )
            }
            return VoiceCommand.SelectSide(pairNumber, side)
        }

        val numbersOnly = normalizeSideValues(extractNumbers(text))
        if (numbersOnly.size >= 4) {
            return VoiceCommand.FillCurrentSide(
                flangeThickness = numbersOnly[0],
                flangeWear = numbersOnly[1],
                flangeSteepness = numbersOnly[2],
                bandageThickness = numbersOnly[3],
            )
        }

        val field = when {
            text.startsWith("гребень") -> MeasurementField.FLANGE_THICKNESS
            text.startsWith("прокат") -> MeasurementField.FLANGE_WEAR
            text.startsWith("крутизна") -> MeasurementField.FLANGE_STEEPNESS
            text.startsWith("бандаж") -> MeasurementField.BANDAGE_THICKNESS
            else -> null
        }
        if (field != null) {
            val value = extractNumber(text)
            if (value != null) return VoiceCommand.SetValue(field, value)
        }

        return VoiceCommand.Unknown(text)
    }

    private fun isStartMeasurementFlow(text: String): Boolean {
        return text == "замер" ||
            text.startsWith("замер ") ||
            text.contains("новый замер") ||
            text.contains("новой замер") ||
            text.contains("новая замер") ||
            text.contains("новый размер") ||
            text.contains("начать замер") ||
            (text.contains("нов") && text.contains("замер")) ||
            (text.contains("нач") && text.contains("замер"))
    }

    private fun resolveSide(text: String): WheelSide? {
        val tokens = text.split(" ").filter { it.isNotBlank() }
        return when {
            tokens.any { it.startsWith("лев") } -> WheelSide.LEFT
            tokens.any { it.startsWith("пра") } -> WheelSide.RIGHT
            else -> null
        }
    }

    private fun normalizeSideValues(numbers: List<Double>): List<Double> {
        if (numbers.size == 5 && numbers[1] in 0.0..9.0 && numbers[2] in 0.0..9.0) {
            val decimal = "${numbers[1].toInt()}.${numbers[2].toInt()}".toDouble()
            return listOf(numbers[0], decimal, numbers[3], numbers[4])
        }
        return numbers
    }

    private fun extractPairNumber(text: String): Int? {
        val prefixTokens = text
            .split(" ")
            .asSequence()
            .map { it.trim('.', ',', ':', ';') }
            .takeWhile { token -> !token.startsWith("лев") && !token.startsWith("пра") && token != "сторона" }
            .filter { it.isNotBlank() }
            .toList()
            .dropWhile { it in startWords }

        prefixTokens
            .asSequence()
            .mapNotNull { token -> ordinals[token] ?: token.toIntOrNull() }
            .firstOrNull()
            ?.let { return it }

        return prefixTokens.indices
            .asSequence()
            .mapNotNull { index -> parseIntegerAt(prefixTokens, index)?.first }
            .firstOrNull()
    }

    private fun isSimpleSideSelection(text: String): Boolean {
        val prefixTokens = text
            .split(" ")
            .asSequence()
            .map { it.trim('.', ',', ':', ';') }
            .takeWhile { token -> !token.startsWith("лев") && !token.startsWith("пра") && token != "сторона" }
            .filter { it.isNotBlank() }
            .toList()
            .dropWhile { it in startWords }

        if (prefixTokens.isEmpty()) return false
        if (prefixTokens.any { it in listOf("гребень", "прокат", "крутизна", "бандаж") }) return false
        return prefixTokens.all { token ->
            ordinals.containsKey(token) || token.toIntOrNull() != null || parseIntegerAt(listOf(token), 0) != null
        }
    }

    private fun afterSideMarker(text: String): String {
        val tokens = text.split(" ")
        val sideIndex = tokens.indexOfFirst { it.startsWith("лев") || it.startsWith("пра") || it == "сторона" }
        return if (sideIndex < 0) text else tokens.drop(sideIndex + 1).joinToString(" ")
    }

    private fun extractNumbers(text: String): List<Double> {
        val cleaned = text
            .replace(Regex("(?<=\\d)\\s*и\\s*(?=\\d)"), ".")
            .replace(Regex("(?<=\\d)\\s*целых\\s*(?=\\d)"), ".")
            .replace(Regex("(?<=\\d)\\s*,\\s*(?=\\d)"), " ")
            .replace(Regex("(?<=\\d)\\s+\\.(?=\\d)"), ".")

        val digitNumbers = Regex("(\\d+(?:\\.\\d+)?)")
            .findAll(cleaned)
            .mapNotNull { it.value.toDoubleOrNull() }
            .toList()
        return if (digitNumbers.size >= 4) digitNumbers else extractSpokenNumbers(cleaned)
    }

    private fun extractSpokenNumbers(text: String): List<Double> {
        val tokens = text
            .replace(".", " ")
            .split(" ")
            .filter { it.isNotBlank() }
        val result = mutableListOf<Double>()
        var index = 0
        while (index < tokens.size) {
            val parsed = parseIntegerAt(tokens, index)
            if (parsed == null) {
                index++
                continue
            }

            val whole = parsed.first
            var nextIndex = parsed.second
            val fractionToken = tokens.getOrNull(nextIndex + 1)
            if (tokens.getOrNull(nextIndex) == "и" && fractionToken != null && units.containsKey(fractionToken)) {
                result += "$whole.${units.getValue(fractionToken)}".toDouble()
                index = nextIndex + 2
            } else {
                result += whole.toDouble()
                index = nextIndex
            }
        }
        return result
    }

    private fun parseIntegerAt(tokens: List<String>, start: Int): Pair<Int, Int>? {
        val token = tokens.getOrNull(start) ?: return null
        token.toIntOrNull()?.let { return it to start + 1 }
        teens[token]?.let { return it to start + 1 }
        units[token]?.let { return it to start + 1 }
        val ten = tens[token] ?: return null
        val nextToken = tokens.getOrNull(start + 1)
        val unit = if (nextToken != null) units[nextToken] else null
        return if (unit != null) {
            (ten + unit) to start + 2
        } else {
            ten to start + 1
        }
    }

    private fun extractNumber(text: String): Double? {
        Regex("(\\d+)(?:\\s*(?:и|целых|[.])\\s*(\\d+))?").find(text)?.let { match ->
            val whole = match.groupValues[1]
            val fraction = match.groupValues.getOrNull(2).orEmpty()
            return if (fraction.isBlank()) whole.toDoubleOrNull() else "$whole.$fraction".toDoubleOrNull()
        }
        return null
    }
}
