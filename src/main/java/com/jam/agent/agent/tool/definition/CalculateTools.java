package com.jam.agent.agent.tool.definition;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class CalculateTools implements AgentToolProvider {

    private final ObjectMapper objectMapper;

    public CalculateTools(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Tool(name = "calculate", description = "使用精确十进制定点数计算基本算术表达式，支持 + - * / 和括号。")
    public String calculate(
            @ToolParam(description = "算术表达式，例如 345678*912345") String expression,
            ToolContext toolContext) throws Exception {
        if (expression == null || expression.length() > 300) {
            throw new IllegalArgumentException("表达式为空或过长。");
        }

        BigDecimal result = new DecimalExpressionParser(expression).parse();
        return objectMapper.writeValueAsString(Map.of(
                "expression", expression,
                "result", result.stripTrailingZeros().toPlainString()));
    }

    /** Minimal decimal parser keeps arithmetic deterministic without evaluating arbitrary code. */
    static final class DecimalExpressionParser {

        private final String input;
        private int index;

        DecimalExpressionParser(String input) {
            this.input = input;
        }

        BigDecimal parse() {
            BigDecimal result = expression();
            skipWhitespace();
            if (index != input.length()) {
                throw new IllegalArgumentException("表达式包含不支持的字符。");
            }
            return result;
        }

        private BigDecimal expression() {
            BigDecimal value = term();
            while (true) {
                skipWhitespace();
                if (take('+')) {
                    value = value.add(term());
                } else if (take('-')) {
                    value = value.subtract(term());
                } else {
                    return value;
                }
            }
        }

        private BigDecimal term() {
            BigDecimal value = factor();
            while (true) {
                skipWhitespace();
                if (take('*')) {
                    value = value.multiply(factor());
                } else if (take('/')) {
                    BigDecimal divisor = factor();
                    if (divisor.signum() == 0) {
                        throw new IllegalArgumentException("不能除以零。");
                    }
                    value = value.divide(divisor, 32, RoundingMode.HALF_UP)
                            .stripTrailingZeros();
                } else {
                    return value;
                }
            }
        }

        private BigDecimal factor() {
            skipWhitespace();
            if (take('+')) {
                return factor();
            }
            if (take('-')) {
                return factor().negate();
            }
            if (take('(')) {
                BigDecimal value = expression();
                if (!take(')')) {
                    throw new IllegalArgumentException("括号不匹配。");
                }
                return value;
            }
            return number();
        }

        private BigDecimal number() {
            int start = index;
            while (index < input.length()
                    && (Character.isDigit(input.charAt(index)) || input.charAt(index) == '.')) {
                index++;
            }
            if (start == index) {
                throw new IllegalArgumentException("需要数字。");
            }
            try {
                return new BigDecimal(input.substring(start, index));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("数字格式无效。");
            }
        }

        private boolean take(char expected) {
            if (index < input.length() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }
    }
}
