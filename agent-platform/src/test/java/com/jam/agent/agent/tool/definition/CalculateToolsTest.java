package com.jam.agent.agent.tool.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CalculateToolsTest {

    @Test
    void multipliesLargeIntegersExactly() {
        assertEquals(
                new BigDecimal("315377594910"),
                new CalculateTools.DecimalExpressionParser("345678*912345").parse());
    }

    @Test
    void supportsParentheses() {
        assertEquals(
                new BigDecimal("14"),
                new CalculateTools.DecimalExpressionParser("2*(3+4)").parse());
    }
}
