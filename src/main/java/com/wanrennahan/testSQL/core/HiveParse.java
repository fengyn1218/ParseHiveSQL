package com.wanrennahan.testSQL.core;

import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.BaseSemanticAnalyzer;
import org.apache.hadoop.hive.ql.parse.HiveParser;

import java.util.*;

import static org.apache.hadoop.hive.ql.parse.BaseSemanticAnalyzer.getUnescapedName;

/**
 * @description: sql解析类
 * @param: null
 * @return:
 * @author: YuNan.Feng
 * @date: 2020/8/514:26
 * @version: 1.0.0
 **/
public class HiveParse {

    //默认数据库名
    private static final String DEFAULT_DATABASES = "DEFAULT";
    private static final String UNKNOWN = "UNKNOWN";
    private Map<String, String> alias = new HashMap<String, String>();
    private Map<String, String> cols = new TreeMap<String, String>();
    private Map<String, String> colAlais = new TreeMap<String, String>();
    //最外层列
    private List<String> list = new ArrayList<>();

    private Set<String> tables = new HashSet<String>();
    private Stack<String> tableNameStack = new Stack<String>();
    private Stack<Oper> operStack = new Stack<Oper>();
    //当前处理表
    private String nowQueryTable = "";
    private Oper oper;
    private boolean joinClause = false;
    private static boolean isIn = false;
    private boolean insertClause = false;


    private enum Oper {
        SELECT, INSERT, DROP, TRUNCATE, LOAD, CREATETABLE, ALTER, INSERT_INTO
    }

    public Set<String> parseIteral(ASTNode ast) {
        Set<String> set = new HashSet<String>();
        prepareToParseCurrentNodeAndChilds(ast);
        set.addAll(parseChildNodes(ast));
        set.addAll(parseCurrentNode(ast, set));
        endParseCurrentNode(ast);
        return set;
    }

    private void endParseCurrentNode(ASTNode ast) {
        if (ast.getToken() != null) {
            switch (ast.getToken().getType()) {
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
            }
        }
    }

    private Set<String> parseCurrentNode(ASTNode ast, Set<String> set) {
        if (ast.getToken() != null) {
            switch (ast.getToken().getType()) {
                case HiveParser.TOK_TABLE_PARTITION:
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

                case HiveParser.TOK_TAB:
                    String tableTab = getUnescapedName((ASTNode) ast.getChild(0));
                    if (oper == Oper.SELECT /*|| oper == Oper.INSERT*/) {
                        nowQueryTable = tableTab;
                    }
                    if (tableTab.split("\\.").length != 2) {
                        tableTab = DEFAULT_DATABASES + "." + tableTab;
                    }
                    tables.add(tableTab + "\t" + oper);
                    break;
                case HiveParser.TOK_TABREF:
                    ASTNode tabTree = (ASTNode) ast.getChild(0);
                    String tableName = (tabTree.getChildCount() == 1) ? getUnescapedName((ASTNode) tabTree.getChild(0))
                            : getUnescapedName((ASTNode) tabTree.getChild(0))
                            + "." + tabTree.getChild(1);
                    if (oper == Oper.SELECT) {
                        if (joinClause && !"".equals(nowQueryTable)) {
                            nowQueryTable += "&" + "";
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
                        alias.put(alia, tableName);
                    }
                    break;
                case HiveParser.TOK_TABLE_OR_COL:
                    if (ast.getParent().getType() != HiveParser.DOT) {
                        String col = ast.getChild(0).getText().toLowerCase();
                        if (alias.get(col) == null
                                && colAlais.get(nowQueryTable + "." + col) == null) {
                            if (nowQueryTable.indexOf("&") > 0) {
                                cols.put(UNKNOWN + "." + col, insertClause == false ? "" : "insertTab");
                                if (insertClause) {
                                    if (!list.contains(UNKNOWN + "." + col)) {
                                        list.add(UNKNOWN + "." + col);
                                    }
                                }
                            } else {
                                cols.put(nowQueryTable + "." + col, insertClause == false ? "" : "insertTab");
                                if (insertClause) {
                                    if (!list.contains(nowQueryTable + "." + col)) {
                                        list.add(nowQueryTable + "." + col);
                                    }
                                }
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
                            cols.put(s + ".*1", insertClause == false ? "" : "insertTab");
                            if (insertClause) {
                                if (!list.contains(s + ".*1")) {
                                    list.add(s + ".*1");
                                }

                            }
                        } else {
                            cols.put(nowQueryTable + ".*", insertClause == false ? "" : "insertTab");
                            if (insertClause) {
                                if (!list.contains(nowQueryTable + ".*")) {
                                    list.add(nowQueryTable + ".*");
                                }

                            }
                        }
                    } else {
                        cols.put(nowQueryTable + ".*", insertClause == false ? "" : "insertTab");
                        if (insertClause) {
                            if (!list.contains(nowQueryTable + ".*")) {
                                list.add(nowQueryTable + ".*");
                            }

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
                        alias.put(tableAlias, aliaReal);
                    }
                    break;
                case HiveParser.TOK_SELEXPR:
                    if (ast.getChild(0).getType() == HiveParser.TOK_TABLE_OR_COL) {
                        String column = ast.getChild(0).getChild(0).getText()
                                .toLowerCase();
                        if (nowQueryTable.indexOf("&") > 0) {
                            cols.put(UNKNOWN + "." + column, "");
                            if (insertClause) {
                                if (!list.contains(UNKNOWN + "." + column)) {
                                    list.add(UNKNOWN + "." + column);
                                }
                            }
                        } else if (colAlais.get(nowQueryTable + "." + column) == null) {
                            cols.put(nowQueryTable + "." + column, insertClause == false ? "" : "insertTab");
                            if (insertClause) {
                                if (!list.contains(nowQueryTable + "." + column)) {
                                    list.add(nowQueryTable + "." + column);
                                }

                            }
                        }
                    } else if (ast.getChild(1) != null) {
                        String columnAlia = ast.getChild(1).getText().toLowerCase();
                        colAlais.put(nowQueryTable + "." + columnAlia, insertClause == false ? "" : "insertTab");

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
                                if (insertClause) {
                                    if (!list.contains(realTable + "." + column)) {
                                        list.add(realTable + "." + column);
                                    }

                                }
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
                //去除where的条件列
                if (child.getToken() != null && child.getToken().getType() != HiveParser.TOK_WHERE) {
                    set.addAll(parseIteral(child));
                } else {
                    continue;
                }
            }
        }
        return set;
    }

    private void prepareToParseCurrentNodeAndChilds(ASTNode ast) {
        if (ast.getToken() != null) {
            switch (ast.getToken().getType()) {
                case HiveParser.TOK_RIGHTOUTERJOIN:
                case HiveParser.TOK_LEFTOUTERJOIN:
                case HiveParser.TOK_JOIN:
                    joinClause = true;
                    break;
                case HiveParser.TOK_QUERY:
                    tableNameStack.push(nowQueryTable);
                    operStack.push(oper);
                    nowQueryTable = "";
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
                    if (!ast.getChild(0).getChild(0).getText().equals("TOK_DIR")) {
                        insertClause = true;
                    }
                    break;
                case HiveParser.TOK_SELECT:
                    tableNameStack.push(nowQueryTable);
                    operStack.push(oper);
                    oper = Oper.SELECT;
                    break;

//                case HiveParser.TOK_TAB:
//                    tableNameStack.push(nowQueryTable);
//                    break;

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

    public void parsehive(ASTNode ast) {
        parseIteral(ast);
    }

    public Set<String> getTables() {
        return tables;
    }

    public Map<String, String> getCols() {
        return cols;
    }

    public List<String> getList() {
        return list;
    }
}