package com.xishanju.testSQL.fildname;

import org.apache.hadoop.hive.ql.parse.*;

import java.util.*;

import static org.apache.hadoop.hive.ql.parse.BaseSemanticAnalyzer.getUnescapedName;

/**
 * @description:
 * @author: 冯雨南
 * @createDate: 2020/7/24
 * @version: 1.0.0
 */
public class TestAST {

    private Set<String> tables = new HashSet<String>();

    private Map<String, String> cols = new TreeMap<String, String>();

    private TestAST.Oper oper;
    private boolean joinClause = false;
    private Stack<String> tableNameStack = new Stack<String>();
    private Stack<Oper> operStack = new Stack<Oper>();
    private String nowQueryTable = "";//定义及处理不清晰，修改为query或from节点对应的table集合或许好点。目前正在查询处理的表可能不止一个。
    String name = "";

    private enum Oper {
        SELECT, INSERT, DROP, TRUNCATE, LOAD, CREATETABLE, ALTER, INSERT_INTO
    }

    public static void main(String[] args) throws ParseException {
        ParseDriver pd = new ParseDriver();
        String sql2 = "insert overwrite table tmp1 PARTITION (partitionkey='2008-08-15') select * from tmp";
        ASTNode ast = pd.parse(sql2);
        System.out.println(ast.toStringTree());
        TestAST testAST = new TestAST();
        testAST.parsehive2(ast);
        Iterator<Map.Entry<String, String>> iterator = testAST.cols.entrySet().iterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }

    }

    public void parsehive2(ASTNode ast) {

        if (ast.getToken() != null) {
            switch (ast.getToken().getType()) {
                case HiveParser.TOK_TABREF:

                    ASTNode tabTree = (ASTNode) ast.getChild(0);
                    String tableName = (tabTree.getChildCount() == 1) ? getUnescapedName((ASTNode) tabTree.getChild(0))
                            : getUnescapedName((ASTNode) tabTree.getChild(0))
                            + "." + tabTree.getChild(1);
                    tables.add(tableName);
                    name = tableName;
                    System.out.println(tableName + oper);
                    break;

                case HiveParser.TOK_TAB://shuchu

                    ASTNode tabTree1 = (ASTNode) ast.getChild(0);
                    String tableName1 = (tabTree1.getChildCount() == 1) ? getUnescapedName((ASTNode) tabTree1.getChild(0))
                            : getUnescapedName((ASTNode) tabTree1.getChild(0))
                            + "." + tabTree1.getChild(1);
                    tables.add(tableName1);
                    name = tableName1;
                    System.out.println(tableName1 + "\t"+oper);
                    break;



                case HiveParser.TOK_TABLE_PARTITION:
                    if (ast.getChildCount() != 2) {
                        String table = getUnescapedName((ASTNode) ast.getChild(0));
                        tables.add(table + "\t" + oper);
                    }
                    break;



                case HiveParser.TOK_TABLE_OR_COL:
                    if (ast.getParent().getType() != HiveParser.DOT) {
                        String col = ast.getChild(0).getText().toLowerCase();
                        cols.put(name + "." + col, "");
                    }

                    break;
                case HiveParser.TOK_ALLCOLREF:
                    cols.put(name + ".*", "");
                    break;

                case HiveParser.TOK_ALTERTABLE:
                    ASTNode alterTableName = (ASTNode) ast.getChild(0).getChild(0);
                    tables.add(alterTableName.getText() + "\t" + oper);
                    break;

                case HiveParser.TOK_ALTERTABLE_ADDPARTS:


                case HiveParser.TOK_ALTERTABLE_RENAME:
                case HiveParser.TOK_ALTERTABLE_ADDCOLS:
            }

        }

        for (int i = 0; i < ast.getChildCount(); i++) {
            ASTNode tabTree = (ASTNode) ast.getChild(i);
            parsehive2(tabTree);
        }
    }


}
