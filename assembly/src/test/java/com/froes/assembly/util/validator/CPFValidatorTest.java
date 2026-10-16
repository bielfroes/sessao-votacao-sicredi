package com.froes.assembly.util.validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CPFValidatorTest {

    @Test
    void acceptsValidCpfWithoutLeadingZeroAndNormalizesIt() {
        assertTrue(CPFValidator.isValidCPF("52998224725"));
        assertEquals("52998224725", CPFValidator.normalize("52998224725"));
    }

    @Test
    void addsLeadingZeroBeforeValidatingCpf() {
        assertEquals("01234567890", CPFValidator.normalize("1234567890"));
        assertTrue(CPFValidator.isValidCPF("1234567890"));
    }

    @Test
    void removesCommonFormattingBeforeValidating() {
        assertEquals("67953008033", CPFValidator.normalize("679.530.080-33"));
        assertTrue(CPFValidator.isValidCPF("679.530.080-33"));
    }

    @Test
    void rejectsInvalidOrIncompleteCpf() {
        assertFalse(CPFValidator.isValidCPF(null));
        assertFalse(CPFValidator.isValidCPF("123456789"));
        assertFalse(CPFValidator.isValidCPF("52998224726"));
        assertNull(CPFValidator.normalize("abc1234567890"));
    }
}
