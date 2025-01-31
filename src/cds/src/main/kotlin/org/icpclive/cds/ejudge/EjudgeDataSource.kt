package org.icpclive.cds.ejudge

import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.common.xmlLoaderService
import org.icpclive.util.child
import org.icpclive.util.children
import org.icpclive.util.guessDatetimeFormat
import org.w3c.dom.Element
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class EjudgeDataSource(val properties: Properties) : FullReloadContestDataSource(5.seconds) {

    override suspend fun loadOnce(): ContestParseResult {
        val element = xmlLoader.loadOnce()
        return parseContestInfo(element.documentElement).also {
            require(it.contestInfo.status == ContestStatus.OVER) {
                "Emulation mode require over contest"
            }
        }
    }

    private fun parseProblemsInfo(doc: Element) = doc
        .child("problems")
        .children().mapIndexed { index, element ->
            ProblemInfo(
                element.getAttribute("short_name"),
                element.getAttribute("short_name"),
                null,
                element.getAttribute("id").toInt(),
                index,
            )
        }.toList()

    private fun parseTeamsInfo(doc: Element) = doc
        .child("users")
        .children().mapIndexed { index, participant ->
            val participantName = participant.getAttribute("name")
            TeamInfo(
                id = index,
                name = participantName,
                shortName = participantName,
                contestSystemId = participant.getAttribute("id"),
                groups = listOf(),
                hashTag = null,
                medias = emptyMap()
            )
        }.toList()

    private fun parseEjudgeTime(time: String): Instant {
        val formattedTime = time
            .replace("/", "-")
            .replace(" ", "T")
        return guessDatetimeFormat(formattedTime)
    }

    private fun parseContestInfo(element: Element) : ContestParseResult {
        val contestLength = element.getAttribute("duration").toLong().seconds
        val startTime = parseEjudgeTime(element.getAttribute("start_time"))
        val currentTime = parseEjudgeTime(element.getAttribute("current_time"))

        val status = when {
            currentTime >= startTime + contestLength -> ContestStatus.OVER
            currentTime < startTime -> ContestStatus.BEFORE
            else -> ContestStatus.RUNNING
        }

        val freezeTime = contestLength - element.getAttribute("fog_time").toLong().seconds
        val teams = parseTeamsInfo(element)
        val teamIdMapping = teams.associateBy({ it.contestSystemId }, { it.id })

        return ContestParseResult(
            contestInfo = ContestInfo(
                status = status,
                resultType = ContestResultType.ICPC,
                startTime = startTime,
                contestLength = contestLength,
                freezeTime = freezeTime,
                problems = parseProblemsInfo(element),
                teams = teams
            ),
            runs = element.child("runs").children().mapNotNull { run ->
                parseRunInfo(run, currentTime - startTime, freezeTime, teamIdMapping)
            }.toList(),
            emptyList()
        )
    }

    private fun parseRunInfo(
        element: Element,
        currentTime: Duration,
        freezeTime: Duration,
        teamIdMapping: Map<String, Int>
    ) : RunInfo? {
        val time = element.getAttribute("time").toLong().seconds + element.getAttribute("nsec").toLong().nanoseconds
        if (time > currentTime) {
            return null
        }

        val teamId = teamIdMapping[element.getAttribute("user_id")]!!
        val runId = element.getAttribute("run_id").toInt()

        val isFrozen = time >= freezeTime
        val result = when {
            isFrozen -> ""
            else -> statusMap.getOrDefault(element.getAttribute("status"), "WA")
        }
        val percentage = when {
            isFrozen -> 0.0
            "" == result -> 0.0
            else -> 1.0
        }

        return RunInfo(
            id = runId,
            isAccepted = "AC" == result,
            isJudged = "" != result,
            isAddingPenalty = "AC" != result && "CE" != result,
            result = result,
            problemId = element.getAttribute("prob_id").toInt(),
            teamId = teamId,
            percentage = percentage,
            time = time,
        )
    }

    private val xmlLoader = xmlLoaderService { properties.getProperty("url") }

    companion object {
        private val statusMap = mapOf(
            "OK" to "AC",
            "CE" to "CE",
            "RT" to "RE",
            "TL" to "TL",
            "PE" to "PE",
            "WA" to "WA",
            "CF" to "FL",
            "PT" to "",
            "AC" to "OK",
            "IG" to "",
            "DQ" to "",
            "PD" to "",
            "ML" to "ML",
            "SE" to "SV",
            "SV" to "",
            "WT" to "IL",
            "PR" to "",
            "RJ" to "",
            "SK" to "",
            "SY" to "",
            "SM" to "",
            "RU" to "",
            "CD" to "",
            "CG" to "",
            "AV" to "",
            "EM" to "",
            "VS" to "",
            "VT" to "",
        )
    }
}
