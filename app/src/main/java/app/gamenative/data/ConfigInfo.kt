package app.gamenative.data

import kotlinx.serialization.Serializable

@Serializable
data class ControllerConfigDetail(
    val publishedFileId: Long,
    val controllerType: String = "",
    val enabledBranches: String = "",
)

@Serializable
data class ConfigInfo(
    val installDir: String = "",
    val launch: List<LaunchInfo> = emptyList(),
    val steamControllerTemplateIndex: Int = 0,
    val steamControllerTouchTemplateIndex: Int = 0,
    val steamControllerConfigDetails: List<ControllerConfigDetail> = emptyList(),
    val steamControllerTouchConfigDetails: List<ControllerConfigDetail> = emptyList(),
    // val steamControllerTouchConfigDetails: TouchConfigDetails,
)
