package ru.depo.zamerykp.data.db

class DatabaseSeeder(private val db: AppDatabase) {
    suspend fun seedIfEmpty() {
        if (db.locomotiveDao().getAll().isNotEmpty()) return

        val now = System.currentTimeMillis()
        val locomotives = listOf(
            SeedLocomotive("ТЭМ2", "767", 6),
            SeedLocomotive("ТЭМ18ДМ", "1072", 6),
            SeedLocomotive("ПЭ2М", "001", 8),
        )

        locomotives.forEach { seed ->
            val locomotiveId = db.locomotiveDao().upsert(
                LocomotiveEntity(
                    series = seed.series,
                    number = seed.number,
                    wheelPairCount = seed.wheelPairCount,
                    comment = "Стартовый справочник",
                    createdOnPhone = false,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            db.wheelPairProfileDao().upsertAll(
                (1..seed.wheelPairCount).map { number ->
                    WheelPairProfileEntity(
                        locomotiveId = locomotiveId,
                        number = number,
                        axisNumber = number,
                    )
                }
            )
        }
    }
}

private data class SeedLocomotive(
    val series: String,
    val number: String,
    val wheelPairCount: Int,
)
