package com.roll_54.roll_mod.economy.vendingblock.gui;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.OptionalLong;

/**
 * Arithmetic for money fields: {@code 832*30+90}, {@code 7^2}, {@code 392/20}. Integers only in,
 * a whole amount out — the result is floored, so {@code 392/20} is 19.
 *
 * <p>Supports {@code + - * / ^}, parentheses, unary minus and whitespace. {@code ^} binds tightest,
 * is right-associative and takes a whole exponent from 0 to {@link #MAX_EXPONENT}. Anything else —
 * a syntax error, division by zero, a negative or out-of-{@code long} result — evaluates to empty.
 */
public final class MoneyExpression {

  public static final int MAX_EXPONENT = 64;
  /** Characters a money field accepts while typing; the parser decides whether they make sense. */
  public static final String INPUT_REGEX = "[0-9+\\-*/^() ]*";

  private static final MathContext CONTEXT = new MathContext(40, RoundingMode.HALF_UP);
  private static final BigDecimal LONG_MAX = BigDecimal.valueOf(Long.MAX_VALUE);
  /** Past this an intermediate value can only overflow the result, so stop before it grows. */
  private static final BigDecimal LIMIT = LONG_MAX.multiply(LONG_MAX);

  private final String text;
  private int pos;

  private MoneyExpression(String text) {
    this.text = text;
  }

  /** The floored value of {@code text}, or empty when it is not a valid non-negative amount. */
  public static OptionalLong evaluate(String text) {
    if (text == null || text.isBlank()) return OptionalLong.empty();
    try {
      MoneyExpression parser = new MoneyExpression(text);
      BigDecimal value = parser.expression();
      parser.skipSpaces();
      if (parser.pos != text.length()) return OptionalLong.empty();
      // Rounded to 20 places first so division noise does not floor away a whole coin:
      // 10/3*3 is 9.999… at 40 digits, and means 10.
      BigDecimal floored = value.setScale(20, RoundingMode.HALF_UP).setScale(0, RoundingMode.FLOOR);
      if (floored.signum() < 0 || floored.compareTo(LONG_MAX) > 0) return OptionalLong.empty();
      return OptionalLong.of(floored.longValueExact());
    } catch (ArithmeticException | IllegalArgumentException e) {
      return OptionalLong.empty();
    }
  }

  /** True when {@code text} is a plain number with nothing to compute. */
  public static boolean isPlain(String text) {
    return text != null && text.trim().matches("\\d+");
  }

  private BigDecimal expression() {
    BigDecimal value = term();
    while (true) {
      if (eat('+')) value = check(value.add(term()));
      else if (eat('-')) value = check(value.subtract(term()));
      else return value;
    }
  }

  private BigDecimal term() {
    BigDecimal value = power();
    while (true) {
      if (eat('*')) value = check(value.multiply(power()));
      else if (eat('/')) {
        BigDecimal divisor = power();
        if (divisor.signum() == 0) throw new ArithmeticException("division by zero");
        value = check(value.divide(divisor, CONTEXT));
      } else return value;
    }
  }

  private BigDecimal power() {
    BigDecimal base = unary();
    if (!eat('^')) return base;
    BigDecimal exponent = power();
    int n;
    try {
      n = exponent.intValueExact();
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException("exponent must be whole");
    }
    if (n < 0 || n > MAX_EXPONENT) throw new IllegalArgumentException("exponent out of range");
    return check(base.pow(n, CONTEXT));
  }

  private BigDecimal unary() {
    if (eat('-')) return unary().negate();
    if (eat('+')) return unary();
    return primary();
  }

  private BigDecimal primary() {
    if (eat('(')) {
      BigDecimal value = expression();
      if (!eat(')')) throw new IllegalArgumentException("missing )");
      return value;
    }
    skipSpaces();
    int start = pos;
    while (pos < text.length() && Character.isDigit(text.charAt(pos))) pos++;
    if (start == pos) throw new IllegalArgumentException("number expected at " + pos);
    if (pos - start > 30) throw new IllegalArgumentException("number too long");
    return new BigDecimal(text.substring(start, pos));
  }

  private boolean eat(char c) {
    skipSpaces();
    if (pos < text.length() && text.charAt(pos) == c) {
      pos++;
      return true;
    }
    return false;
  }

  private void skipSpaces() {
    while (pos < text.length() && text.charAt(pos) == ' ') pos++;
  }

  private static BigDecimal check(BigDecimal value) {
    if (value.abs().compareTo(LIMIT) > 0) throw new ArithmeticException("too large");
    return value;
  }
}
