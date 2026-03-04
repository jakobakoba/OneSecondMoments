package com.bor96dev.edit

import com.bor96dev.edit.presentation.formatLocationText
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class EditFormattersTest : StringSpec({
    "город и страна" {
        formatLocationText(city = "Moscow", country = "Russia") shouldBe "Moscow / Russia"
    }

    "показываем только если знаем и город и страну" {
        formatLocationText(city = "Moscow", country = null) shouldBe null
        formatLocationText(city = null, country = "Russia") shouldBe null
        formatLocationText(city = "Moscow", country = "   ") shouldBe null
        formatLocationText(city = "   ", country = "Russia") shouldBe null
    }

    "пробелы обрезаются" {
        formatLocationText(city = "  Moscow ", country = " Russia  ") shouldBe "Moscow / Russia"
    }

    "пустое остаётся пустым" {
        formatLocationText(city = null, country = null) shouldBe null
        formatLocationText(city = "   ", country = "   ") shouldBe null
    }
})