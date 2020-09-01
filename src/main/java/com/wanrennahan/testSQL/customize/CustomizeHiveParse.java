package com.wanrennahan.testSQL.customize;


import org.antlr.runtime.TokenStream;
import org.apache.hadoop.hive.ql.parse.HiveParser;

/**
 * @description:
 * @author: YuNan.Feng
 * @createDate: 2020/7/28
 * @version: 1.0.0
 */
public class CustomizeHiveParse extends HiveParser {
    public CustomizeHiveParse(TokenStream input) {
        super(input);
    }

    @Override
    protected boolean useSQL11ReservedKeywordsForIdentifier() {
        return true;
    }
}
