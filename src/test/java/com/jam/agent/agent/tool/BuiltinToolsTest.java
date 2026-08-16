package com.jam.agent.agent.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BuiltinToolsTest {
    @Test
    void multipliesLargeIntegersExactly() {
        assertEquals(new BigDecimal("315377594910"),
                new BuiltinTools.DecimalExpressionParser("345678*912345").parse());
    }

    @Test
    void respectsOperatorPrecedenceAndParentheses() {
        assertEquals(new BigDecimal("14"), new BuiltinTools.DecimalExpressionParser("2*(3+4)").parse());
    }
}
