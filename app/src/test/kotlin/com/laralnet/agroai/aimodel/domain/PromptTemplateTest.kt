package com.laralnet.agroai.aimodel.domain

import com.laralnet.agroai.aimodel.domain.model.PromptTemplate
import com.laralnet.agroai.aimodel.domain.model.PromptWarningLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptTemplateTest {

    @Test
    fun `photoAnalysisDefault() has MEDIUM warning level`() {
        val template = PromptTemplate.photoAnalysisDefault()
        assertEquals(PromptWarningLevel.MEDIUM, template.warningLevel)
    }

    @Test
    fun `photoAnalysisDefault() content equals defaultContent initially`() {
        val template = PromptTemplate.photoAnalysisDefault()
        assertEquals(template.defaultContent, template.content)
    }

    @Test
    fun `photoAnalysisDefault() is editable`() {
        val template = PromptTemplate.photoAnalysisDefault()
        assertTrue(template.isEditable)
    }

    @Test
    fun `photoAnalysisDefault() is not customized by default`() {
        val template = PromptTemplate.photoAnalysisDefault()
        assertFalse(template.isCustomized)
    }

    @Test
    fun `weatherAdjustmentDefault() has LOW warning level`() {
        val template = PromptTemplate.weatherAdjustmentDefault()
        assertEquals(PromptWarningLevel.LOW, template.warningLevel)
    }

    @Test
    fun `photoAnalysisDefault() has a non-blank name`() {
        val template = PromptTemplate.photoAnalysisDefault()
        assertTrue(template.name.isNotBlank())
    }

    @Test
    fun `copy with isCustomized true marks template as customized`() {
        val template = PromptTemplate.photoAnalysisDefault()
        val modified = template.copy(content = "custom prompt", isCustomized = true)
        assertTrue(modified.isCustomized)
        assertEquals("custom prompt", modified.content)
        // defaultContent should remain unchanged for reset support
        assertEquals(template.defaultContent, modified.defaultContent)
    }
}
