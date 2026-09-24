package com.harmony.tokoharmony.data.local.mapper

import com.harmony.tokoharmony.core.database.entity.DigitalTransactionEntity
import com.harmony.tokoharmony.domain.model.DigitalTransaction

fun DigitalTransaction.toEntity(): DigitalTransactionEntity = DigitalTransactionEntity.fromDomain(this)
