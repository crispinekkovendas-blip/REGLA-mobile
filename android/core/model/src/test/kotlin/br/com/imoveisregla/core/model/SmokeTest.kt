package br.com.imoveisregla.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmokeTest {
    @Test fun formatsBrlPrice() = assertEquals("R$ 1.850.000", formatPrice(1_850_000, Currency.BRL))
    @Test fun validCpf() = assertTrue(isValidCpf("529.982.247-25"))
    @Test fun refCode() = assertEquals("RG-0042", listingRef(42))
}
