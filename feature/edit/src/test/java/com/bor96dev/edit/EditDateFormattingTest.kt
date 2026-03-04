package com.bor96dev.edit

import com.bor96dev.edit.presentation.toDateString
import com.bor96dev.edit.presentation.toFormattedDateString
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.TimeZone

class EditDateFormattingTest : StringSpec({
    val originalTimeZone = TimeZone.getDefault()

    beforeSpec {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    afterSpec {
        TimeZone.setDefault(originalTimeZone)
    }

    "дата из миллисекунд: yyyy-MM-dd" {
        0L.toDateString() shouldBe "1970-01-01"
        1_700_000_000_000L.toDateString() shouldBe "2023-11-14"
    }

    "формат даты для оверлея: MMM dd yyyy" {
        0L.toFormattedDateString() shouldBe "Jan 01 1970"
    }
})