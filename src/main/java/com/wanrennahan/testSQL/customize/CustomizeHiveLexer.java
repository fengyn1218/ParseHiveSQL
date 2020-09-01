package com.wanrennahan.testSQL.customize;


import org.antlr.runtime.CharStream;
import org.antlr.runtime.RecognizerSharedState;
import org.apache.hadoop.hive.ql.parse.HiveLexer;

/**
 * @description:
 * @author: YuNan.Feng
 * @createDate: 2020/7/28
 * @version: 1.0.0
 */
public class CustomizeHiveLexer extends HiveLexer {
   // private final ArrayList<ParseError> errors;

    public CustomizeHiveLexer() {
        super();
       // errors = new ArrayList<ParseError>();
    }
    public CustomizeHiveLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);
    }

    public CustomizeHiveLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }

    @Override
    protected boolean allowQuotedId() {
        return true;
    }
}
