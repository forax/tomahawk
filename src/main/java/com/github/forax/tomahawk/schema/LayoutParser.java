package com.github.forax.tomahawk.schema;

import com.github.forax.tomahawk.schema.Layout.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.joining;

final class LayoutParser {
  @SuppressWarnings("HardcodedFileSeparator")
  enum Token {
    COMMA("(\\,)"),
    LEFT_PARENS("(\\()"), RIGHT_PARENS("(\\))"),
    STRING("\"([^\"]+)\""),
    IDENTIFIER("(\\p{Alnum}+)"),
    SPACE("( \\t\\n+)")
    ;

    private final String pattern;

    Token(String pattern) {
      this.pattern = pattern;
    }

    private static final List<Token> TOKENS = List.of(values());
    private static final Pattern PATTERN = compile(TOKENS.stream().map(t -> t.pattern).collect(joining("|")));
  }

  static final class Lexer {
    private final Matcher matcher;
    private int line = 1;
    private String identifier;

    public Lexer(String text) {
      matcher = Token.PATTERN.matcher(text);
    }

    private Token findToken() {
      if (!matcher.find()) {
        return null;
      }
      String group = null;
      int i = 1;
      for(; i < matcher.groupCount(); i++) {
        group = matcher.group(i);
        if (group != null) {
          break;
        }
      }
      if (group == null) {
        throw new IllegalStateException("parsing error: unknown character, at line " + line);
      }
      var token = Token.TOKENS.get(i - 1);
      //System.err.println("found " + token + " #" + group + "#");
      identifier = group;
      return token;
    }

    public Token nextToken() {
      for(;;) {
        var token = findToken();
        if (token != Token.SPACE) {
          return token;
        }
        line += identifier.chars().filter(c -> c != '\n').count();
      }
    }

    public String expect(Token expected) {
      var token = nextToken();
      if (token != expected) {
        throw new IllegalStateException("parsing error: expect " + expected + " but found " + token + " instead, at line " + line);
      }
      return identifier;
    }
  }

  public static Layout parse(String text) {
    var lexer = new Lexer(text);
    return parseLayout(lexer);
  }

  private static Layout parseLayout(Lexer lexer) {
    var identifier = lexer.expect(Token.IDENTIFIER);
    return switch(identifier) {
      case "struct" -> parseStructLayout(lexer);
      case "list" -> parseListLayout(lexer);
      case "string" -> parseStringLayout(lexer);
      default -> parsePrimitiveLayout(lexer, identifier);
    };
  }

  private static boolean parseNullable(Lexer lexer) {
    var bool = lexer.expect(Token.IDENTIFIER);
    return switch (bool) {
      case "true" -> true;
      case "false" -> false;
      default -> throw new IllegalStateException("parsing error: expect true or false but found " + bool + " instead, at line " + lexer.line);
    };
  }

  private static Layout.PrimitiveLayout parsePrimitiveLayout(Lexer lexer, String identifier) {
    lexer.expect(Token.LEFT_PARENS);
    var nullable = parseNullable(lexer);
    lexer.expect(Token.RIGHT_PARENS);
    return switch(identifier) {
      case "u1" -> Layout.u1(nullable);
      case "byte8" -> Layout.byte8(nullable);
      case "short16" -> Layout.short16(nullable);
      case "char16" -> Layout.char16(nullable);
      case "int32" -> Layout.int32(nullable);
      case "float32" -> Layout.float32(nullable);
      case "long64" -> Layout.long64(nullable);
      case "double64" -> Layout.double64(nullable);
      default -> throw new IllegalStateException("parsing error: expect a primitive layout but found " + identifier + " instead, at line " + lexer.line);
    };
  }

  private static Layout parseStringLayout(Lexer lexer) {
    lexer.expect(Token.LEFT_PARENS);
    var nullable = parseNullable(lexer);
    lexer.expect(Token.RIGHT_PARENS);
    return Layout.string(nullable);
  }

  private static Layout parseListLayout(Lexer lexer) {
    lexer.expect(Token.LEFT_PARENS);
    var nullable = parseNullable(lexer);
    lexer.expect(Token.COMMA);
    var element = parseLayout(lexer);
    lexer.expect(Token.RIGHT_PARENS);
    return Layout.list(nullable, element);
  }

  private static Layout parseStructLayout(Lexer lexer) {
    lexer.expect(Token.LEFT_PARENS);
    var nullable = parseNullable(lexer);
    var token = lexer.nextToken();
    switch(token) {
      case RIGHT_PARENS -> { return Layout.struct(nullable); }
      case COMMA -> { }
      default -> throw new IllegalStateException("parsing error: expect either a closing parenthesis or a comma but found " + token + " instead, at line " + lexer.line);
    }
    var fields = new ArrayList<Field>();
    loop: for(;;) {
      var identifier = lexer.expect(Token.IDENTIFIER);
      if (!identifier.equals("field")) {
        throw new IllegalStateException("parsing error: expect a field but found " + identifier + " instead, at line " + lexer.line);
      }
      lexer.expect(Token.LEFT_PARENS);
      var name = lexer.expect(Token.STRING);
      lexer.expect(Token.COMMA);
      var layout = parseLayout(lexer);
      lexer.expect(Token.RIGHT_PARENS);
      fields.add(new Field(name, layout));
      token = lexer.nextToken();
      switch(token) {
        case COMMA -> { continue loop; }
        case RIGHT_PARENS -> { break loop; }
        default -> throw new IllegalStateException("parsing error: expect a comma or a right parenthesis but found " + token + " instead, at line " + lexer.line);
      }
    }
    return Layout.struct(nullable, fields.toArray(Field[]::new));
  }
}
