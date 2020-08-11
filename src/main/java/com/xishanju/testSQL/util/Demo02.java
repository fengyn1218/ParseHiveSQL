package com.xishanju.testSQL.util;


//import parse.CustomizeParserDriver;

import org.apache.hadoop.hive.ql.parse.*;

import java.util.*;

import static org.apache.hadoop.hive.ql.parse.BaseSemanticAnalyzer.getUnescapedName;

/**
 * 目的：获取AST中的表，列，以及对其所做的操作，如SELECT,INSERT
 * 重点：获取SELECT操作中的表和列的相关操作。其他操作这判断到表级别。
 * 实现思路：对AST深度优先遍历，遇到操作的token则判断当前的操作，
 * 遇到TOK_TAB或TOK_TABREF则判断出当前操作的表，遇到子句则压栈当前处理，处理子句。
 * 子句处理完，栈弹出。
 */
public class Demo02 {

    private static final String UNKNOWN = "UNKNOWN";
    private Map<String, String> alias = new HashMap<String, String>();
    private Map<String, String> cols = new TreeMap<String, String>();
    private Map<String, String> colAlais = new TreeMap<String, String>();

    private Stack<String> colAlaisStack = new Stack<>();

    private Set<String> tables = new HashSet<String>();
    private Stack<String> tableNameStack = new Stack<String>();
    private Stack<Oper> operStack = new Stack<Oper>();
    private String nowQueryTable = "";//定义及处理不清晰，修改为query或from节点对应的table集合或许好点。目前正在查询处理的表可能不止一个。
    private Oper oper;
    private boolean joinClause = false;
    private boolean insertClause = false;
    private static boolean isIn = false;

    private static final String DEFAULT_DATABASES = "DEFAULT";

    private enum Oper {
        SELECT, INSERT, DROP, TRUNCATE, LOAD, CREATETABLE, ALTER, INSERT_INTO
    }

    public Set<String> parseIteral(ASTNode ast) {
        Set<String> set = new HashSet<String>();//当前查询所对应到的表集合
        prepareToParseCurrentNodeAndChilds(ast);
        set.addAll(parseChildNodes(ast));
        set.addAll(parseCurrentNode(ast, set));
        endParseCurrentNode(ast);
        return set;
    }

    private void endParseCurrentNode(ASTNode ast) {
        if (ast.getToken() != null) {
            switch (ast.getToken().getType()) {//join 从句结束，跳出join
                case HiveParser.TOK_RIGHTOUTERJOIN:
                case HiveParser.TOK_LEFTOUTERJOIN:
                case HiveParser.TOK_JOIN:
                    joinClause = false;
                    break;
                case HiveParser.TOK_SELECT:
                    nowQueryTable = tableNameStack.pop();
                    oper = operStack.pop();
                    break;
                case HiveParser.TOK_INSERT:
                    nowQueryTable = tableNameStack.pop();
                    oper = operStack.pop();
                    insertClause = false;
                    break;
                case HiveParser.TOK_QUERY:
                    if (isIn) {
                        nowQueryTable = tableNameStack.pop();
                        oper = operStack.pop();
                    }
                    break;
                case HiveParser.TOK_INSERT_INTO:
                    nowQueryTable = tableNameStack.pop();
                    oper = operStack.pop();
                    break;

//                   nowQueryTable = tableNameStack.pop();
//                   oper = operStack.pop();


                //     case HiveParser.TOK_WHERE:

            }
        }
    }

    private Set<String> parseCurrentNode(ASTNode ast, Set<String> set) {
        if (ast.getToken() != null) {
            switch (ast.getToken().getType()) {
                case HiveParser.TOK_TABLE_PARTITION:
                    //   case HiveParser.TOK_TABNAME:
                    if (ast.getChildCount() != 2) {
                        String table = getUnescapedName((ASTNode) ast.getChild(0));
                        if (oper == Oper.SELECT) {
                            nowQueryTable = table;
                        }

                        if (table.split("\\.").length != 2) {
                            table = DEFAULT_DATABASES + "." + table;
                        }

                        tables.add(table + "\t" + oper);
                    }
                    break;

                case HiveParser.TOK_TAB:// outputTable
                    String tableTab = getUnescapedName((ASTNode) ast.getChild(0));
                    if (oper == Oper.SELECT) {
                        nowQueryTable = tableTab;
                    }


                    if (tableTab.split("\\.").length != 2) {
                        tableTab = DEFAULT_DATABASES + "." + tableTab;
                    }

                    tables.add(tableTab + "\t" + oper);
                    break;
                case HiveParser.TOK_TABREF:// inputTable
                    ASTNode tabTree = (ASTNode) ast.getChild(0);
                    String tableName = (tabTree.getChildCount() == 1) ? getUnescapedName((ASTNode) tabTree.getChild(0))
                            : getUnescapedName((ASTNode) tabTree.getChild(0))
                            + "." + tabTree.getChild(1);
                    if (oper == Oper.SELECT) {
                        if (joinClause && !"".equals(nowQueryTable)) {
                            nowQueryTable += "&" + "";//
                        } else {
                            nowQueryTable = tableName;
                        }
                        set.add(tableName);
                    }

                    if (tableName.split("\\.").length != 2) {
                        tableName = DEFAULT_DATABASES + "." + tableName;
                    }

                    tables.add(tableName + "\t" + oper);
                    if (ast.getChild(1) != null) {
                        String alia = ast.getChild(1).getText().toLowerCase();
                        alias.put(alia, tableName);//sql6 p别名在tabref只对应为一个表的别名。
                    }
                    break;
                case HiveParser.TOK_TABLE_OR_COL:

                    if (ast.getParent().getType() != HiveParser.DOT) {
                        String col = ast.getChild(0).getText().toLowerCase();
                        if (alias.get(col) == null && colAlais.get(nowQueryTable + "." + col) == null) {
                            if (nowQueryTable.indexOf("&") > 0) {//sql23
                                cols.put(UNKNOWN + "." + col, insertClause == false ? "" : "insertTab");
                            } else {
                                cols.put(nowQueryTable + "." + col, insertClause == false ? "" : "insertTab");
                            }
                        }
                    }

                    break;
                case HiveParser.TOK_ALLCOLREF:
                    if (ast.getChild(0) != null) {
                        if (ast.getChild(0).getChild(0).toString() != null) {
                            String bieming = ast.getChild(0).getChild(0).toString();
                            //根据别名获取表名
                            String s = alias.get(bieming);
                            cols.put(s + ".*", insertClause == false ? "" : "insertTab");
                        } else {
                            cols.put(nowQueryTable + ".*", insertClause == false ? "" : "insertTab");
                        }

                    }

                    break;
                case HiveParser.TOK_SUBQUERY:
                    if (ast.getChildCount() == 2) {
                        String tableAlias = unescapeIdentifier(ast.getChild(1)
                                .getText());
                        String aliaReal = "";
                        for (String table : set) {
                            aliaReal += table + "&";
                        }
                        if (aliaReal.length() != 0) {
                            aliaReal = aliaReal.substring(0, aliaReal.length() - 1);
                        }
//                    alias.put(tableAlias, nowQueryTable);//sql22
                        alias.put(tableAlias, aliaReal);//sql6
//                    alias.put(tableAlias, "");// just store alias
                    }
                    break;

                case HiveParser.TOK_SELEXPR:
                    if (ast.getChild(0).getType() == HiveParser.TOK_TABLE_OR_COL) {
                        String column = ast.getChild(0).getChild(0).getText()
                                .toLowerCase();
                        if (nowQueryTable.indexOf("&") > 0) {
                            cols.put(UNKNOWN + "." + column, insertClause == false ? "" : "insertTab");
                        } else if (colAlais.get(nowQueryTable + "." + column) == null) {
                            cols.put(nowQueryTable + "." + column, insertClause == false ? "" : "insertTab");
                        }
                    } else if (ast.getChild(1) != null) {// TOK_SELEXPR (+
                        // (TOK_TABLE_OR_COL id)
                        // 1) dd
                        String columnAlia = ast.getChild(1).getText().toLowerCase();
                        colAlais.put(nowQueryTable + "." + columnAlia, "");
                    }
                    break;


                case HiveParser.DOT:
                    if (ast.getType() == HiveParser.DOT) {
                        if (ast.getChildCount() == 2) {
                            if (ast.getChild(0).getType() == HiveParser.TOK_TABLE_OR_COL
                                    && ast.getChild(0).getChildCount() == 1
                                    && ast.getChild(1).getType() == HiveParser.Identifier) {
                                String alia = BaseSemanticAnalyzer
                                        .unescapeIdentifier(ast.getChild(0)
                                                .getChild(0).getText()
                                                .toLowerCase());
                                String column = BaseSemanticAnalyzer
                                        .unescapeIdentifier(ast.getChild(1)
                                                .getText().toLowerCase());
                                String realTable = null;
                                if (!tables.contains(alia + "\t" + oper)
                                        && alias.get(alia) == null) {
                                    alias.put(alia, nowQueryTable);
                                }
                                if (tables.contains(alia + "\t" + oper)) {
                                    realTable = alia;
                                } else if (alias.get(alia) != null) {
                                    realTable = alias.get(alia);
                                }
                                if (realTable == null || realTable.length() == 0 || realTable.indexOf("&") > 0) {
                                    realTable = UNKNOWN;
                                    //   realTable = nowQueryTable;
                                }
                                cols.put(realTable + "." + column, insertClause == false ? "" : "insertTab");

                            }
                        }
                    }
                    break;

                case HiveParser.TOK_ALTERTABLE:
                    ASTNode alterTableName = (ASTNode) ast.getChild(0).getChild(0);

                    String tName = alterTableName.getText();

                    if (tName.split("\\.").length != 2) {
                        tName = DEFAULT_DATABASES + "." + tName;
                    }
                    tables.add(tName + "\t" + oper);
                    break;

                case HiveParser.TOK_ALTERTABLE_ADDPARTS:


                case HiveParser.TOK_ALTERTABLE_RENAME:
                case HiveParser.TOK_ALTERTABLE_ADDCOLS:
//                    ASTNode alterTableName = (ASTNode) ast.getChild(0).getChild(0);
//
//
//                    tables.add(alterTableName.getText() + "\t" + oper);
//                   break;
            }
        }
        return set;
    }

    private Set<String> parseChildNodes(ASTNode ast) {
        Set<String> set = new HashSet<String>();
        int numCh = ast.getChildCount();
        if (numCh > 0) {
            for (int num = 0; num < numCh; num++) {
                ASTNode child = (ASTNode) ast.getChild(num);
//                set.addAll(parseIteral(child));
                if(child.getToken() != null && child.getToken().getType() != HiveParser.TOK_WHERE){
                    set.addAll(parseIteral(child));
                }else {
                    continue;
                }
            }
        }
        return set;
    }

    private void prepareToParseCurrentNodeAndChilds(ASTNode ast) {
//        System.out.println(ast.toStringTree());
        if (ast.getToken() != null) {
            switch (ast.getToken().getType()) {//join 从句开始
                case HiveParser.TOK_RIGHTOUTERJOIN:
                case HiveParser.TOK_LEFTOUTERJOIN:
                case HiveParser.TOK_JOIN:
                    joinClause = true;
                    break;
                case HiveParser.TOK_QUERY:
                    tableNameStack.push(nowQueryTable);
                    operStack.push(oper);
                    nowQueryTable = "";//sql22
                    oper = Oper.SELECT;
                    break;
                case HiveParser.TOK_INSERT_INTO:
                    tableNameStack.push(nowQueryTable);
                    operStack.push(oper);
                    oper = Oper.INSERT_INTO;
                    break;
                case HiveParser.TOK_INSERT:
                    tableNameStack.push(nowQueryTable);
                    operStack.push(oper);
                    oper = Oper.INSERT;
                    if(ast.getChild(0).getChild(0).getText() != "TOK_DIR"){
//                        System.out.println(ast.getChild(0).getChild(0).getText());
                        insertClause = true;
                    }
                    break;
                case HiveParser.TOK_SELECT:
                    tableNameStack.push(nowQueryTable);
                    operStack.push(oper);
                    oper = Oper.SELECT;
                    break;
                case HiveParser.TOK_SUBQUERY_OP:
                    isIn = true;
                    break;
                case HiveParser.TOK_DROPTABLE:
                    oper = Oper.DROP;
                    break;
                case HiveParser.TOK_TRUNCATETABLE:
                    oper = Oper.TRUNCATE;
                    break;
                case HiveParser.TOK_LOAD:
                    oper = Oper.LOAD;
                    break;
                case HiveParser.TOK_CREATETABLE:
                    oper = Oper.CREATETABLE;
                    break;
            }
            if (ast.getToken() != null
                    && ast.getToken().getType() >= HiveParser.TOK_ALTERDATABASE_PROPERTIES
                    && ast.getToken().getType() <= HiveParser.TOK_ALTERVIEW_RENAME) {
                oper = Oper.ALTER;
            }
        }
    }

    public static String unescapeIdentifier(String val) {
        if (val == null) {
            return null;
        }
        if (val.charAt(0) == '`' && val.charAt(val.length() - 1) == '`') {
            val = val.substring(1, val.length() - 1);
        }
        return val;
    }

    private void output(Map<String, String> map) {
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            System.out.println(key + "\t" + map.get(key));
        }
    }

    public void parsehive(ASTNode ast) {
        parseIteral(ast);
//        System.out.println("***************表***************");
//        for (String table : tables) {
//
//            System.out.println(table);
//        }
//        System.out.println("***************列***************");
//        output(cols);
//        System.out.println("***************别名***************");
//        output(alias);
    }

    public Set<String> getTables() {
        return tables;
    }

    public Map<String, String> getCols() {
        return cols;
    }

    public static void main(String[] args) throws ParseException {
        Demo02 demo01 = new Demo02();

        String sql2 = "INSERT OVERWRITE TABLE tmp_jxsj_questionnaire_1\n" +
                "select t2.* \tfrom tmp_jxsj_questionnaire_1  t2,\n" +
                "\t(\n" +
                "\t  select t1.account_id , sum(t1.recharge_money) AS sum_rechage_money\n" +
                "\t\tfrom dw.dw_app t1\n" +
                "\t  where t1.dt >= '2016-09-20'\n" +
                "\t  and t1.dt <= '2016-11-02'\n" +
                "\t  and t1.app_id='16873'\n" +
                "\t  and t1.msgtype = 'role.recharge' \t \n" +
                "\t  group by t1.account_id\n" +
                "\t) t3\n" +
                "\twhere t3.account_id = t2.account_id";
        ParseDriver parseDriver = new ParseDriver();
        //  OverrideParserDriver parseDriver = new OverrideParserDriver();
        ASTNode tree = null;
        try {
            tree = parseDriver.parse(sql2);
        } catch (ParseException e) {
            System.out.println("方法1失败");
//            CustomizeParserDriver parseDriver1 = new CustomizeParserDriver();
//            tree = parseDriver1.parse(sql2);
        }

        demo01.parsehive(tree);
        Set<String> tables = demo01.getTables();
        for (String table : tables) {
            System.out.println(table);
        }
        System.out.println();
        for (String s : demo01.getCols().keySet()) {
            System.out.println(s + "  >>  " + demo01.getCols().get(s));
        }


      //  System.out.println(sql2);


        System.out.println(tree.toStringTree());


    }
}