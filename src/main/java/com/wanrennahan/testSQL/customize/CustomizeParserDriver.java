package com.wanrennahan.testSQL.customize;


import org.antlr.runtime.CharStream;
import org.antlr.runtime.NoViableAltException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenRewriteStream;
import org.apache.hadoop.hive.ql.Context;
import org.apache.hadoop.hive.ql.parse.*;

import java.util.ArrayList;

/**
 * @description:
 * @author: YuNan.Feng
 * @createDate: 2020/7/28
 * @version: 1.0.0
 */
public class CustomizeParserDriver extends ParseDriver {

    @Override
    public ASTNode parse(String command) throws ParseException {
        return parse(command, null);
    }

    @Override
    public ASTNode parse(String command, Context ctx) throws ParseException {
        return parse(command, ctx, true);
    }

    @Override
    public ASTNode parse(String command, Context ctx, boolean setTokenRewriteStream) throws ParseException {

        HiveLexerX lexer = new HiveLexerX(new ANTLRNoCaseStringStream(command));
        TokenRewriteStream tokens = new TokenRewriteStream(lexer);

        CustomizeHiveParse parse = new CustomizeHiveParse(tokens);

        parse.setTreeAdaptor(adaptor);

        HiveParser.statement_return r = null;
        try {
            r = parse.statement();
        } catch (RecognitionException e) {
            e.printStackTrace();
        }
        ASTNode tree = (ASTNode) r.getTree();
        tree.setUnknownTokenBoundaries();
        return tree;

    }


    public class CustomizeHiveLexerX extends CustomizeHiveLexer {

        private final ArrayList<ParseError> errors;

        public CustomizeHiveLexerX() {
            super();
            errors = new ArrayList<ParseError>();
        }

        public CustomizeHiveLexerX(CharStream input) {
            super(input);
            errors = new ArrayList<ParseError>();
        }

        @Override
        public void displayRecognitionError(String[] tokenNames,
                                            RecognitionException e) {

//             errors.add(new ParseError(this, e, tokenNames));
        }

        @Override
        public String getErrorMessage(RecognitionException e, String[] tokenNames) {
            String msg = null;

            if (e instanceof NoViableAltException) {
                @SuppressWarnings("unused")
                NoViableAltException nvae = (NoViableAltException) e;
                // for development, can add
                // "decision=<<"+nvae.grammarDecisionDescription+">>"
                // and "(decision="+nvae.decisionNumber+") and
                // "state "+nvae.stateNumber
                msg = "character " + getCharErrorDisplay(e.c) + " not supported here";
            } else {
                msg = super.getErrorMessage(e, tokenNames);
            }

            return msg;
        }

        public ArrayList<ParseError> getErrors() {
            return errors;
        }

    }
}
