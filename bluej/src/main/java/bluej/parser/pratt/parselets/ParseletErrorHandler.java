package bluej.parser.pratt.parselets;

import bluej.parser.lexer.LocatableToken;
import bluej.parser.lexer.JavaTokenTypes;
import bluej.parser.pratt.KotlinPrattParser;
import bluej.parser.pratt.NodeFactory;

/**
 * Standardized error handling utility for parselets in the Kotlin Pratt parser.
 *
 * <p>This class provides consistent error reporting methods and standard error
 * message formats across all parselet implementations. It ensures that error
 * handling follows established patterns and provides helpful diagnostic information
 * for developers using BlueJ.</p>
 *
 * <p><strong>Usage Pattern:</strong></p>
 * <pre>{@code
 * public ParsedNode parse(KotlinPrattParser parser, LocatableToken token) {
 *     if (!ParseletErrorHandler.validateToken(parser, token, "literal parselet")) {
 *         return null;
 *     }
 *
 *     if (!ParseletErrorHandler.validateTokenType(parser, token, JavaTokenTypes.STRING_LITERAL)) {
 *         return null;
 *     }
 *
 *     NodeFactory factory = ParseletErrorHandler.getNodeFactory(parser, token);
 *     if (factory == null) {
 *         return null;
 *     }
 *
 *     // ... continue with parsing logic
 * }
 * }</pre>
 *
 * @author BlueJ Kotlin Implementation
 * @since 2025-01-24
 */
public final class ParseletErrorHandler {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ParseletErrorHandler() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    // ===== TOKEN VALIDATION METHODS =====

    /**
     * Validates that a token is not null and reports an appropriate error if it is.
     *
     * @param parser The parser instance for error reporting
     * @param token The token to validate
     * @param context Context description for the error message (e.g., "literal parselet")
     * @return true if token is valid, false if null
     */
    public static boolean validateToken(KotlinPrattParser parser, LocatableToken token, String context) {
        if (token == null) {
            parser.error("Null token in " + context, null);
            return false;
        }
        return true;
    }

    /**
     * Validates that a token has the expected type.
     *
     * @param parser The parser instance for error reporting
     * @param token The token to validate (must not be null)
     * @param expectedType The expected token type
     * @return true if token type matches, false otherwise
     */
    public static boolean validateTokenType(KotlinPrattParser parser, LocatableToken token, int expectedType) {
        if (token.getType() != expectedType) {
            String expected = getTokenTypeName(expectedType);
            String actual = getTokenTypeName(token.getType());
            parser.error("Expected " + expected + " but got " + actual, token);
            return false;
        }
        return true;
    }

    /**
     * Validates that a token is one of several acceptable types.
     *
     * @param parser The parser instance for error reporting
     * @param token The token to validate (must not be null)
     * @param acceptableTypes Array of acceptable token types
     * @param context Context description for the error message
     * @return true if token type is acceptable, false otherwise
     */
    public static boolean validateTokenTypes(KotlinPrattParser parser, LocatableToken token,
                                           int[] acceptableTypes, String context) {
        int tokenType = token.getType();
        for (int acceptableType : acceptableTypes) {
            if (tokenType == acceptableType) {
                return true;
            }
        }

        StringBuilder expectedTypes = new StringBuilder();
        for (int i = 0; i < acceptableTypes.length; i++) {
            if (i > 0) {
                expectedTypes.append(i == acceptableTypes.length - 1 ? " or " : ", ");
            }
            expectedTypes.append(getTokenTypeName(acceptableTypes[i]));
        }

        String actual = getTokenTypeName(tokenType);
        parser.error("Expected " + expectedTypes + " for " + context + " but got " + actual, token);
        return false;
    }

    // ===== OPERAND VALIDATION METHODS =====

    /**
     * Validates that a left operand is present for infix operations.
     *
     * @param parser The parser instance for error reporting
     * @param leftOperand The left operand to validate
     * @param operatorToken The operator token for context
     * @param operatorDescription Description of the operator (e.g., "binary operator", "member access")
     * @return true if left operand is valid, false if null
     */
    public static boolean validateLeftOperand(KotlinPrattParser parser, Object leftOperand,
                                            LocatableToken operatorToken, String operatorDescription) {
        if (leftOperand == null) {
            parser.error("Missing left operand for " + operatorDescription, operatorToken);
            return false;
        }
        return true;
    }

    /**
     * Validates that a right operand was successfully parsed.
     *
     * @param parser The parser instance for error reporting
     * @param rightOperand The right operand to validate
     * @param operatorToken The operator token for context
     * @param operatorDescription Description of the operator
     * @return true if right operand is valid, false if null
     */
    public static boolean validateRightOperand(KotlinPrattParser parser, Object rightOperand,
                                             LocatableToken operatorToken, String operatorDescription) {
        if (rightOperand == null) {
            String operatorText = (operatorToken != null) ? operatorToken.getText() : "unknown";
            parser.error("Missing right operand for " + operatorDescription + " '" + operatorText + "'", operatorToken);
            return false;
        }
        return true;
    }

    /**
     * Validates that an operand was successfully parsed for prefix operations.
     *
     * @param parser The parser instance for error reporting
     * @param operand The operand to validate
     * @param operatorToken The operator token for context
     * @param operatorDescription Description of the operator (e.g., "unary minus", "logical not")
     * @return true if operand is valid, false if null
     */
    public static boolean validatePrefixOperand(KotlinPrattParser parser, Object operand,
                                              LocatableToken operatorToken, String operatorDescription) {
        if (operand == null) {
            String operatorText = (operatorToken != null) ? operatorToken.getText() : "unknown";
            parser.error("Missing operand for " + operatorDescription + " '" + operatorText + "'", operatorToken);
            return false;
        }
        return true;
    }

    // ===== EXPRESSION VALIDATION METHODS =====

    /**
     * Validates that an expression was successfully parsed.
     *
     * @param parser The parser instance for error reporting
     * @param expression The expression result to validate
     * @param context Context description (e.g., "array index", "function argument")
     * @return true if expression is valid, false if null
     */
    public static boolean validateExpression(KotlinPrattParser parser, Object expression, String context) {
        if (expression == null) {
            parser.error("Expected expression for " + context, parser.peek());
            return false;
        }
        return true;
    }

    /**
     * Reports an unexpected end of input error.
     *
     * @param parser The parser instance for error reporting
     * @param context Context description (e.g., "array access", "function call")
     */
    public static void reportUnexpectedEndOfInput(KotlinPrattParser parser, String context) {
        parser.error("Unexpected end of input in " + context, null);
    }

    /**
     * Reports a missing closing delimiter error.
     *
     * @param parser The parser instance for error reporting
     * @param expectedDelimiter The expected closing delimiter (e.g., ")", "]", "}")
     * @param context Context description
     * @param actualToken The token that was found instead (may be null)
     */
    public static void reportMissingDelimiter(KotlinPrattParser parser, String expectedDelimiter,
                                            String context, LocatableToken actualToken) {
        if (actualToken == null) {
            parser.error("Expected '" + expectedDelimiter + "' to close " + context, null);
        } else {
            parser.error("Expected '" + expectedDelimiter + "' to close " + context +
                        ", found: " + actualToken.getText(), actualToken);
        }
    }

    // ===== NODE FACTORY VALIDATION =====

    /**
     * Retrieves and validates the NodeFactory from the parser.
     *
     * @param parser The parser instance
     * @param contextToken Token for error reporting context
     * @return The NodeFactory if available, null if unavailable (error reported)
     */
    public static NodeFactory getNodeFactory(KotlinPrattParser parser, LocatableToken contextToken) {
        NodeFactory nodeFactory = parser.getNodeFactory();
        if (nodeFactory == null) {
            parser.error("NodeFactory not available for AST node creation", contextToken);
            return null;
        }
        return nodeFactory;
    }

    /**
     * Safely creates a node using the NodeFactory and handles any exceptions.
     *
     * @param nodeFactory The factory to use for node creation
     * @param nodeCreator Function that creates the node (should throw no checked exceptions)
     * @param parser The parser for error reporting
     * @param contextToken Token for error context
     * @param nodeDescription Description of the node being created (e.g., "literal node", "binary operator node")
     * @return The created node, or null if creation failed
     */
    @FunctionalInterface
    public interface NodeCreator<T> {
        T create() throws Exception;
    }

    public static <T> T safeCreateNode(NodeFactory nodeFactory, NodeCreator<T> nodeCreator,
                                      KotlinPrattParser parser, LocatableToken contextToken,
                                      String nodeDescription) {
        try {
            return nodeCreator.create();
        } catch (Exception e) {
            parser.error("Failed to create " + nodeDescription + ": " + e.getMessage(), contextToken);
            return null;
        }
    }

    // ===== TOKEN TYPE UTILITY METHODS =====

    /**
     * Provides human-readable names for token types to improve error messages.
     *
     * @param tokenType The token type constant from JavaTokenTypes
     * @return A descriptive name for the token type
     */
    public static String getTokenTypeName(int tokenType) {
        switch (tokenType) {
            // Literals
            case JavaTokenTypes.NUM_INT: return "integer literal";
            case JavaTokenTypes.NUM_LONG: return "long literal";
            case JavaTokenTypes.NUM_FLOAT: return "float literal";
            case JavaTokenTypes.NUM_DOUBLE: return "double literal";
            case JavaTokenTypes.CHAR_LITERAL: return "character literal";
            case JavaTokenTypes.STRING_LITERAL: return "string literal";
            case JavaTokenTypes.LITERAL_true: return "'true'";
            case JavaTokenTypes.LITERAL_false: return "'false'";
            case JavaTokenTypes.LITERAL_null: return "'null'";

            // Identifiers and keywords
            case JavaTokenTypes.IDENT: return "identifier";
            case JavaTokenTypes.LITERAL_this: return "'this'";
            case JavaTokenTypes.LITERAL_super: return "'super'";

            // Operators
            case JavaTokenTypes.PLUS: return "'+'";
            case JavaTokenTypes.MINUS: return "'-'";
            case JavaTokenTypes.STAR: return "'*'";
            case JavaTokenTypes.DIV: return "'/'";
            case JavaTokenTypes.MOD: return "'%'";
            case JavaTokenTypes.ASSIGN: return "'='";
            case JavaTokenTypes.EQUAL: return "'=='";
            case JavaTokenTypes.NOT_EQUAL: return "'!='";
            case JavaTokenTypes.LT: return "'<'";
            case JavaTokenTypes.GT: return "'>'";
            case JavaTokenTypes.LE: return "'<='";
            case JavaTokenTypes.GE: return "'>='";
            case JavaTokenTypes.LAND: return "'&&'";
            case JavaTokenTypes.LOR: return "'||'";
            case JavaTokenTypes.LNOT: return "'!'";
            case JavaTokenTypes.INC: return "'++'";
            case JavaTokenTypes.DEC: return "'--'";

            // Delimiters
            case JavaTokenTypes.LPAREN: return "'('";
            case JavaTokenTypes.RPAREN: return "')'";
            case JavaTokenTypes.LBRACK: return "'['";
            case JavaTokenTypes.RBRACK: return "']'";
            case JavaTokenTypes.LCURLY: return "'{'";
            case JavaTokenTypes.RCURLY: return "'}'";
            case JavaTokenTypes.SEMI: return "';'";
            case JavaTokenTypes.COMMA: return "','";
            case JavaTokenTypes.DOT: return "'.'";
            case JavaTokenTypes.QUESTION: return "'?'";
            case JavaTokenTypes.COLON: return "':'";

            // Special cases
            case JavaTokenTypes.EOF: return "end of file";
            case JavaTokenTypes.WHITESPACE: return "whitespace";
            case JavaTokenTypes.INVALID: return "invalid token";

            default:
                return "token type " + tokenType;
        }
    }

    // ===== CONTEXT-SPECIFIC ERROR METHODS =====

    /**
     * Reports an error for invalid binary operators.
     *
     * @param parser The parser instance
     * @param token The token that was expected to be a binary operator
     */
    public static void reportInvalidBinaryOperator(KotlinPrattParser parser, LocatableToken token) {
        parser.error("Expected binary operator, got: " + getTokenTypeName(token.getType()), token);
    }

    /**
     * Reports an error for invalid unary operators.
     *
     * @param parser The parser instance
     * @param token The token that was expected to be a unary operator
     * @param prefix true if this is a prefix operator, false if postfix
     */
    public static void reportInvalidUnaryOperator(KotlinPrattParser parser, LocatableToken token, boolean prefix) {
        String operatorType = prefix ? "prefix" : "postfix";
        parser.error("Expected " + operatorType + " operator, got: " + getTokenTypeName(token.getType()), token);
    }

    /**
     * Reports an error for invalid literal tokens.
     *
     * @param parser The parser instance
     * @param token The token that was expected to be a literal
     */
    public static void reportInvalidLiteral(KotlinPrattParser parser, LocatableToken token) {
        parser.error("Expected literal token, got: " + getTokenTypeName(token.getType()), token);
    }

    /**
     * Reports an error for invalid identifiers.
     *
     * @param parser The parser instance
     * @param token The token that was expected to be an identifier
     */
    public static void reportInvalidIdentifier(KotlinPrattParser parser, LocatableToken token) {
        parser.error("Expected identifier, got: " + getTokenTypeName(token.getType()), token);
    }

    /**
     * Reports a generic syntax error with context.
     *
     * @param parser The parser instance
     * @param message The error message
     * @param token The token associated with the error (may be null)
     */
    public static void reportSyntaxError(KotlinPrattParser parser, String message, LocatableToken token) {
        parser.error("Syntax error: " + message, token);
    }
}
