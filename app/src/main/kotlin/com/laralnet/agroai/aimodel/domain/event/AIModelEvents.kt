package com.laralnet.agroai.aimodel.domain.event

import com.laralnet.agroai.aimodel.domain.model.ModelVariant
import com.laralnet.agroai.core.domain.event.DomainEvent

class ModelDownloadStarted(val modelId: String, val variant: ModelVariant) : DomainEvent()
class ModelActivated(val modelId: String) : DomainEvent()
class ModelDeleted(val modelId: String) : DomainEvent()
class PromptTemplateUpdated(val templateId: String, val templateName: String) : DomainEvent()
