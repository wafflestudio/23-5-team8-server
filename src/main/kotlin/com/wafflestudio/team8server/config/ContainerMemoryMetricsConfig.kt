package com.wafflestudio.team8server.config

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.readText

@Configuration
class ContainerMemoryMetricsConfig {
    @Bean
    fun containerMemoryMetrics(): MeterBinder =
        MeterBinder { registry ->
            val cgroupReader = CgroupMemoryReader()
            val processReader = ProcessMemoryReader()

            byteGauge(
                registry = registry,
                name = "container.memory.current",
                description = "Current cgroup memory usage.",
            ) { cgroupReader.current() }
            byteGauge(
                registry = registry,
                name = "container.memory.working_set",
                description = "Estimated cgroup working set memory. This is memory.current minus inactive_file.",
            ) { cgroupReader.workingSet() }
            byteGauge(
                registry = registry,
                name = "container.memory.limit",
                description = "Cgroup memory limit.",
            ) { cgroupReader.limit() }

            listOf(
                "anon",
                "file",
                "kernel_stack",
                "pagetables",
                "slab",
                "sock",
                "shmem",
                "inactive_file",
            ).forEach { id ->
                byteGauge(
                    registry = registry,
                    name = "container.memory.stat",
                    description = "Selected values from cgroup memory.stat.",
                    id = id,
                ) { cgroupReader.stat(id) }
            }

            listOf("VmRSS", "VmSize").forEach { id ->
                byteGauge(
                    registry = registry,
                    name = "process.memory.status",
                    description = "Selected values from /proc/self/status.",
                    id = id,
                ) { processReader.status(id) }
            }

            listOf(
                "Rss",
                "Pss",
                "Shared_Clean",
                "Shared_Dirty",
                "Private_Clean",
                "Private_Dirty",
                "Anonymous",
                "Swap",
            ).forEach { id ->
                byteGauge(
                    registry = registry,
                    name = "process.memory.smaps_rollup",
                    description = "Selected values from /proc/self/smaps_rollup.",
                    id = id,
                ) { processReader.smapsRollup(id) }
            }
        }

    private fun byteGauge(
        registry: MeterRegistry,
        name: String,
        description: String,
        id: String? = null,
        value: () -> Long?,
    ) {
        val builder =
            Gauge
                .builder(name, value) { it.invoke()?.toDouble() ?: Double.NaN }
                .description(description)
                .baseUnit("bytes")
                .strongReference(true)

        if (id == null) {
            builder.register(registry)
        } else {
            builder.tag("id", id).register(registry)
        }
    }
}

private class CgroupMemoryReader {
    private val v2Root = Path.of("/sys/fs/cgroup")
    private val v1MemoryRoot = Path.of("/sys/fs/cgroup/memory")

    fun current(): Long? =
        readLong(v2Root.resolve("memory.current"))
            ?: readLong(v1MemoryRoot.resolve("memory.usage_in_bytes"))

    fun limit(): Long? =
        readLongOrMax(v2Root.resolve("memory.max"))
            ?: readLong(v1MemoryRoot.resolve("memory.limit_in_bytes"))

    fun workingSet(): Long? {
        val current = current() ?: return null
        val inactiveFile = stat("inactive_file") ?: return current
        return (current - inactiveFile).coerceAtLeast(0)
    }

    fun stat(id: String): Long? =
        memoryStat()[id]
            ?: v1StatFallback(id)

    private fun memoryStat(): Map<String, Long> =
        readStatFile(v2Root.resolve("memory.stat"))
            ?: readStatFile(v1MemoryRoot.resolve("memory.stat"))
            ?: emptyMap()

    private fun v1StatFallback(id: String): Long? =
        when (id) {
            "anon" -> memoryStat()["total_rss"]
            "file" -> memoryStat()["total_cache"]
            "inactive_file" -> memoryStat()["total_inactive_file"]
            else -> null
        }

    private fun readStatFile(path: Path): Map<String, Long>? {
        if (!path.exists()) return null

        return runCatching {
            path
                .readLines()
                .mapNotNull { line ->
                    val parts = line.split(" ")
                    if (parts.size != 2) return@mapNotNull null
                    parts[0] to parts[1].toLong()
                }.toMap()
        }.getOrNull()
    }

    private fun readLong(path: Path): Long? {
        if (!path.exists()) return null

        return runCatching {
            path.readText().trim().toLong()
        }.getOrNull()
    }

    private fun readLongOrMax(path: Path): Long? {
        if (!path.exists()) return null

        return runCatching {
            path
                .readText()
                .trim()
                .takeUnless { it == "max" }
                ?.toLong()
        }.getOrNull()
    }
}

private class ProcessMemoryReader {
    fun status(id: String): Long? = readKbValue(Path.of("/proc/self/status"), id)

    fun smapsRollup(id: String): Long? = readKbValue(Path.of("/proc/self/smaps_rollup"), id)

    private fun readKbValue(
        path: Path,
        id: String,
    ): Long? {
        if (!Files.isReadable(path)) return null

        return runCatching {
            val line =
                (
                    path
                        .readLines()
                        .firstOrNull { it.startsWith("$id:") }
                ) ?: return@runCatching null
            val value =
                (
                    line
                        .trim()
                        .split(Regex("\\s+"))
                        .getOrNull(1)
                        ?.toLong()
                ) ?: return@runCatching null

            value * 1024
        }.getOrNull()
    }
}
