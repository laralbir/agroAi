package com.laralnet.agroai.ui.aimodel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.laralnet.agroai.aimodel.domain.model.AIModel
import com.laralnet.agroai.aimodel.domain.model.DownloadState
import com.laralnet.agroai.aimodel.domain.model.GemmaVersion
import com.laralnet.agroai.aimodel.domain.model.HuggingFaceCredential
import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.ui.screens.aimodel.ModelManagementContent
import com.laralnet.agroai.ui.screens.aimodel.ModelManagementViewModel
import com.laralnet.agroai.ui.theme.AgroAITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModelManagementScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(uiState: ModelManagementViewModel.UiState) {
        composeRule.setContent {
            AgroAITheme {
                ModelManagementContent(
                    uiState = uiState,
                    onNavigateBack = {},
                    onConnect = {},
                    onDownload = {},
                    onActivate = {},
                    onDelete = {},
                    onTest = { _, _ -> },
                    onReconnect = {}
                )
            }
        }
    }

    @Test
    fun `shows HuggingFace banner when no credential`() {
        setContent(ModelManagementViewModel.UiState(hfCredential = null))
        composeRule.onNodeWithTag("model_hf_banner").assertIsDisplayed()
    }

    @Test
    fun `hides HuggingFace banner when credential is present`() {
        setContent(ModelManagementViewModel.UiState(hfCredential = fakeCredential()))
        composeRule.onNodeWithTag("model_hf_banner").assertDoesNotExist()
    }

    @Test
    fun `shows variant card for each row`() {
        val row = row(ModelVariant.GEMMA3_1B, DownloadState.NOT_DOWNLOADED)
        setContent(ModelManagementViewModel.UiState(rows = listOf(row)))
        composeRule
            .onNodeWithTag("model_variant_card_${ModelVariant.GEMMA3_1B.name}")
            .assertIsDisplayed()
    }

    @Test
    fun `shows variant card in DOWNLOADING state`() {
        val row = ModelManagementViewModel.ModelRowState(
            variant = ModelVariant.GEMMA3_1B,
            model = fakeModel(DownloadState.DOWNLOADING),
            downloadProgress = 55
        )
        setContent(ModelManagementViewModel.UiState(rows = listOf(row)))
        composeRule
            .onNodeWithTag("model_variant_card_${ModelVariant.GEMMA3_1B.name}")
            .assertIsDisplayed()
    }

    @Test
    fun `shows variant card in DOWNLOADED state`() {
        val row = row(ModelVariant.GEMMA3_1B, DownloadState.DOWNLOADED)
        setContent(ModelManagementViewModel.UiState(rows = listOf(row)))
        composeRule
            .onNodeWithTag("model_variant_card_${ModelVariant.GEMMA3_1B.name}")
            .assertIsDisplayed()
    }

    @Test
    fun `shows variant card in FAILED state`() {
        val row = ModelManagementViewModel.ModelRowState(
            variant = ModelVariant.GEMMA3_1B,
            model = fakeModel(DownloadState.FAILED).copy(lastError = "Download failed")
        )
        setContent(ModelManagementViewModel.UiState(rows = listOf(row)))
        composeRule
            .onNodeWithTag("model_variant_card_${ModelVariant.GEMMA3_1B.name}")
            .assertIsDisplayed()
    }

    @Test
    fun `shows multiple variant cards`() {
        val rows = listOf(
            row(ModelVariant.GEMMA3_1B, DownloadState.NOT_DOWNLOADED),
            row(ModelVariant.GEMMA4_E2B, DownloadState.DOWNLOADED)
        )
        setContent(ModelManagementViewModel.UiState(rows = rows))
        composeRule.onNodeWithTag("model_variant_card_${ModelVariant.GEMMA3_1B.name}").assertIsDisplayed()
        composeRule.onNodeWithTag("model_variant_card_${ModelVariant.GEMMA4_E2B.name}").assertIsDisplayed()
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun row(variant: ModelVariant, state: DownloadState) =
        ModelManagementViewModel.ModelRowState(
            variant = variant,
            model = if (state == DownloadState.NOT_DOWNLOADED) null else fakeModel(state)
        )

    private fun fakeModel(state: DownloadState) = AIModel(
        variant = ModelVariant.GEMMA3_1B,
        version = GemmaVersion.GEMMA_3,
        downloadState = state
    )

    private fun fakeCredential() = HuggingFaceCredential(
        accessToken = "fake_token",
        username = "testuser"
    )
}
