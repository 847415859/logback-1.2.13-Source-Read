/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2015, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.pattern.parser;

import java.util.List;
import java.util.ArrayList;

import ch.qos.logback.core.CoreConstants;
import static ch.qos.logback.core.CoreConstants.CURLY_LEFT;
import static ch.qos.logback.core.CoreConstants.ESCAPE_CHAR;

import ch.qos.logback.core.pattern.util.IEscapeUtil;
import ch.qos.logback.core.pattern.util.RegularEscapeUtil;
import ch.qos.logback.core.pattern.util.RestrictedEscapeUtil;
import ch.qos.logback.core.spi.ScanException;

/**
 * <p>
 * Return a steady stream of tokens.
 * <p/>
 * <p/>
 * <p>
 * The returned tokens are one of: LITERAL, '%', FORMAT_MODIFIER, SIMPLE_KEYWORD, COMPOSITE_KEYWORD
 * OPTION, LEFT_PARENTHESIS, and RIGHT_PARENTHESIS.
 * </p>
 * <p/>
 * <p>
 * The '\' character is used as escape. It can be used to escape '_', '%', '('
 * and '('.
 * <p>
 * <p/>
 * <p>
 * Note that there is no EOS token returned.
 * </p>
 */
class TokenStream {

    enum TokenizerState {
        LITERAL_STATE, // 文字状态
        FORMAT_MODIFIER_STATE, // 格式修饰符状态
        KEYWORD_STATE,   // 关键字状态
        OPTION_STATE,    // 选项状态
        RIGHT_PARENTHESIS_STATE  // 右括号状态
    }

    final String pattern;
    final int patternLength;
    final IEscapeUtil escapeUtil;

    final IEscapeUtil optionEscapeUtil = new RestrictedEscapeUtil();

    TokenizerState state = TokenizerState.LITERAL_STATE;
    int pointer = 0;

    // this variant should be used for testing purposes only
    TokenStream(String pattern) {
        this(pattern, new RegularEscapeUtil());
    }

    TokenStream(String pattern, IEscapeUtil escapeUtil) {
        if (pattern == null || pattern.length() == 0) {
            throw new IllegalArgumentException("null or empty pattern string not allowed");
        }
        this.pattern = pattern;
        patternLength = pattern.length();
        this.escapeUtil = escapeUtil;
    }

    List tokenize() throws ScanException {
        List<Token> tokenList = new ArrayList<Token>();
        StringBuffer buf = new StringBuffer();

        while (pointer < patternLength) {
            char c = pattern.charAt(pointer);
            pointer++;

            switch (state) {
            case LITERAL_STATE:             // 文字状态
                handleLiteralState(c, tokenList, buf);
                break;
            case FORMAT_MODIFIER_STATE:     // token 修饰符状态
                handleFormatModifierState(c, tokenList, buf);
                break;
            case OPTION_STATE:              // 可选状态
                processOption(c, tokenList, buf);
                break;
            case KEYWORD_STATE:             // 关键词状态
                handleKeywordState(c, tokenList, buf);
                break;
            case RIGHT_PARENTHESIS_STATE:   // 右括号状态
                handleRightParenthesisState(c, tokenList, buf);
                break;

            default:
            }
        }

        // EOS
        switch (state) {
        case LITERAL_STATE:
            addValuedToken(Token.LITERAL, buf, tokenList);
            break;
        case KEYWORD_STATE:
            tokenList.add(new Token(Token.SIMPLE_KEYWORD, buf.toString()));
            break;
        case RIGHT_PARENTHESIS_STATE:
            tokenList.add(Token.RIGHT_PARENTHESIS_TOKEN);
            break;

        case FORMAT_MODIFIER_STATE:
        case OPTION_STATE:
            throw new ScanException("Unexpected end of pattern string");
        }

        return tokenList;
    }

    private void handleRightParenthesisState(char c, List<Token> tokenList, StringBuffer buf) {
        tokenList.add(Token.RIGHT_PARENTHESIS_TOKEN);
        switch (c) {
        case CoreConstants.RIGHT_PARENTHESIS_CHAR:
            break;
        case CURLY_LEFT:
            state = TokenizerState.OPTION_STATE;
            break;
        case ESCAPE_CHAR:
            escape("%{}", buf);
            state = TokenizerState.LITERAL_STATE;
            break;
        default:
            buf.append(c);
            state = TokenizerState.LITERAL_STATE;
        }
    }

    private void processOption(char c, List<Token> tokenList, StringBuffer buf) throws ScanException {
        OptionTokenizer ot = new OptionTokenizer(this);
        ot.tokenize(c, tokenList);
    }

    private void handleFormatModifierState(char c, List<Token> tokenList, StringBuffer buf) {
        if (c == CoreConstants.LEFT_PARENTHESIS_CHAR) {    // 是否是  '('
            // 添加格式化修饰符的Token
            addValuedToken(Token.FORMAT_MODIFIER, buf, tokenList);
            tokenList.add(Token.BARE_COMPOSITE_KEYWORD_TOKEN);
            state = TokenizerState.LITERAL_STATE;
        } else if (Character.isJavaIdentifierStart(c)) {        // 确定指定的字符（Unicode代码点）是否为字母表
            // 添加格式化修饰符的Token
            addValuedToken(Token.FORMAT_MODIFIER, buf, tokenList);
            // 设置为关键字token
            state = TokenizerState.KEYWORD_STATE;
            // 将需要处理的token放到buf中
            buf.append(c);
        } else {
            buf.append(c);
        }
    }

    private void handleLiteralState(char c, List<Token> tokenList, StringBuffer buf) {
        switch (c) {
        case ESCAPE_CHAR:   //  ‘\\’ 号
            escape("%()", buf);
            break;

        case CoreConstants.PERCENT_CHAR:        // '%' 号
            // 添加 Token值
            addValuedToken(Token.LITERAL, buf, tokenList);
            // 添加 % Token
            tokenList.add(Token.PERCENT_TOKEN);
            // 设置为格式化状态
            state = TokenizerState.FORMAT_MODIFIER_STATE;
            break;

        case CoreConstants.RIGHT_PARENTHESIS_CHAR:      // ')' 号
            // 添加 Token值
            addValuedToken(Token.LITERAL, buf, tokenList);
            // 设置状态为 右括号状态
            state = TokenizerState.RIGHT_PARENTHESIS_STATE;
            break;

        default:
            buf.append(c);
        }
    }

    private void handleKeywordState(char c, List<Token> tokenList, StringBuffer buf) {
        // 确定指定的字符是否可以作为第一个字符以外的Java标识符的一部分
        if (Character.isJavaIdentifierPart(c)) {        //
            buf.append(c);
            // ‘{’
        } else if (c == CURLY_LEFT) {
            addValuedToken(Token.SIMPLE_KEYWORD, buf, tokenList);
            state = TokenizerState.OPTION_STATE;
        // '('
        } else if (c == CoreConstants.LEFT_PARENTHESIS_CHAR) {
            addValuedToken(Token.COMPOSITE_KEYWORD, buf, tokenList);
            state = TokenizerState.LITERAL_STATE;
        // '%'
        } else if (c == CoreConstants.PERCENT_CHAR) {
            addValuedToken(Token.SIMPLE_KEYWORD, buf, tokenList);
            tokenList.add(Token.PERCENT_TOKEN);
            state = TokenizerState.FORMAT_MODIFIER_STATE;
        //  ')'
        } else if (c == CoreConstants.RIGHT_PARENTHESIS_CHAR) {
            addValuedToken(Token.SIMPLE_KEYWORD, buf, tokenList);
            state = TokenizerState.RIGHT_PARENTHESIS_STATE;
        } else {
            // 关键字处理
            addValuedToken(Token.SIMPLE_KEYWORD, buf, tokenList);
            // ‘\\’ 转义符
            if (c == ESCAPE_CHAR) {
                if ((pointer < patternLength)) {
                    char next = pattern.charAt(pointer++);
                    escapeUtil.escape("%()", buf, next, pointer);
                }
            } else {
                buf.append(c);
            }
            state = TokenizerState.LITERAL_STATE;
        }
    }

    void escape(String escapeChars, StringBuffer buf) {
        if ((pointer < patternLength)) {
            char next = pattern.charAt(pointer++);
            escapeUtil.escape(escapeChars, buf, next, pointer);
        }
    }

    void optionEscape(String escapeChars, StringBuffer buf) {
        if ((pointer < patternLength)) {
            char next = pattern.charAt(pointer++);
            optionEscapeUtil.escape(escapeChars, buf, next, pointer);
        }
    }

    /**
     * 添加 token值
     * @param type      类型
     * @param buf       token值
     * @param tokenList token列表
     */
    private void addValuedToken(int type, StringBuffer buf, List<Token> tokenList) {
        if (buf.length() > 0) {
            tokenList.add(new Token(type, buf.toString()));
            buf.setLength(0);
        }
    }
}