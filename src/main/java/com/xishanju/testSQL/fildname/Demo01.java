//package com.xishanju.testSQL.fildname;
//
//
//import org.apache.hadoop.hive.ql.parse.*;
//
//import java.io.IOException;
//import java.util.*;
//
//import static org.apache.hadoop.hive.ql.parse.BaseSemanticAnalyzer.getUnescapedName;
//
///**
// * 目的：获取AST中的表，列，以及对其所做的操作，如SELECT,INSERT
// * 重点：获取SELECT操作中的表和列的相关操作。其他操作这判断到表级别。
// * 实现思路：对AST深度优先遍历，遇到操作的token则判断当前的操作，
// * 遇到TOK_TAB或TOK_TABREF则判断出当前操作的表，遇到子句则压栈当前处理，处理子句。
// * 子句处理完，栈弹出。
// */
//public class Demo01 {
//
//    private class entity {
//        public entity(Map<String, String> tempMap) {
//            this.tempMap = tempMap;
//        }
//
//        private Map<String, String> tempMap = new HashMap<>();
//    }
//
//    private static final String UNKNOWN = "UNKNOWN";
//    private Map<String, String> alias = new HashMap<String, String>();
//    private Map<String, String> cols = new TreeMap<String, String>();
//    private Map<String, String> colAlais = new TreeMap<String, String>();
//
//    private Stack<Map<String, String>> colAlaisStack = new Stack<>();
//    private String nowColAlais = "";
//
//    private Set<String> tables = new HashSet<String>();
//    private Stack<String> tableNameStack = new Stack<String>();
//    private Stack<Oper> operStack = new Stack<Oper>();
//    private String nowQueryTable = "";//定义及处理不清晰，修改为query或from节点对应的table集合或许好点。目前正在查询处理的表可能不止一个。
//    private Oper oper;
//    private boolean joinClause = false;
//    private static boolean isIn = false;
//
//    private static final String DEFAULT_DATABASES = "DEFAULT";
//
//    private enum Oper {
//        SELECT, INSERT, DROP, TRUNCATE, LOAD, CREATETABLE, ALTER, INSERT_INTO
//    }
//
//    public Set<String> parseIteral(ASTNode ast) {
//        Set<String> set = new HashSet<String>();//当前查询所对应到的表集合
//        prepareToParseCurrentNodeAndChilds(ast);
//        set.addAll(parseChildNodes(ast));
//        set.addAll(parseCurrentNode(ast, set));
//        endParseCurrentNode(ast);
//        return set;
//    }
//
//    private void endParseCurrentNode(ASTNode ast) {
//        if (ast.getToken() != null) {
//            switch (ast.getToken().getType()) {//join 从句结束，跳出join
//                case HiveParser.TOK_RIGHTOUTERJOIN:
//                case HiveParser.TOK_LEFTOUTERJOIN:
//                case HiveParser.TOK_JOIN:
//                    joinClause = false;
//                    break;
////case Hiveparser.TOK_un
//
//                case HiveParser.TOK_SELECT:
//                    nowQueryTable = tableNameStack.pop();
//                    nowColAlais = colAlaisStack.pop().keySet().toString();
//
//                    oper = operStack.pop();
//                    break;
//                case HiveParser.TOK_INSERT:
//                    nowQueryTable = tableNameStack.pop();
//                    nowColAlais = colAlaisStack.pop().keySet().toString();
//                    oper = operStack.pop();
//                    break;
//                case HiveParser.TOK_QUERY:
//                    if (isIn) {
//                        nowQueryTable = tableNameStack.pop();
//                        nowColAlais = colAlaisStack.pop().keySet().toString();
//                        oper = operStack.pop();
//                    }
//
//                    break;
//                case HiveParser.TOK_INSERT_INTO:
//                    nowQueryTable = tableNameStack.pop();
//                    nowColAlais = colAlaisStack.pop().keySet().toString();
//                    oper = operStack.pop();
//                    break;
//
////                   nowQueryTable = tableNameStack.pop();
////                   oper = operStack.pop();
//
//
//                //     case HiveParser.TOK_WHERE:
//
//            }
//        }
//    }
//
//    private Set<String> parseCurrentNode(ASTNode ast, Set<String> set) {
//        if (ast.getToken() != null) {
//            switch (ast.getToken().getType()) {
//                case HiveParser.TOK_TABLE_PARTITION:
//                    //   case HiveParser.TOK_TABNAME:
//                    if (ast.getChildCount() != 2) {
//                        String table = getUnescapedName((ASTNode) ast.getChild(0));
//                        if (oper == Oper.SELECT) {
//                            nowQueryTable = table;
//                        }
//
//                        if (table.split("\\.").length != 2) {
//                            table = DEFAULT_DATABASES + "." + table;
//                        }
//
//                        tables.add(table + "\t" + oper);
//                    }
//                    break;
//
//                case HiveParser.TOK_TAB:// outputTable
//                    String tableTab = getUnescapedName((ASTNode) ast.getChild(0));
//                    if (oper == Oper.SELECT) {
//                        nowQueryTable = tableTab;
//                    }
//
//
//                    if (tableTab.split("\\.").length != 2) {
//                        tableTab = DEFAULT_DATABASES + "." + tableTab;
//                    }
//
//                    tables.add(tableTab + "\t" + oper);
//                    break;
//                case HiveParser.TOK_TABREF:// inputTable
//                    ASTNode tabTree = (ASTNode) ast.getChild(0);
//                    String tableName = (tabTree.getChildCount() == 1) ? getUnescapedName((ASTNode) tabTree.getChild(0))
//                            : getUnescapedName((ASTNode) tabTree.getChild(0))
//                            + "." + tabTree.getChild(1);
//                    if (oper == Oper.SELECT) {
//                        if (joinClause && !"".equals(nowQueryTable)) {
//                            nowQueryTable += "&" + "";//
//                        } else {
//                            nowQueryTable = tableName;
//                        }
//                        set.add(tableName);
//                    }
//
//                    if (tableName.split("\\.").length != 2) {
//                        tableName = DEFAULT_DATABASES + "." + tableName;
//                    }
//
//                    tables.add(tableName + "\t" + oper);
//                    if (ast.getChild(1) != null) {
//                        String alia = ast.getChild(1).getText().toLowerCase();
//                        alias.put(alia, tableName);//sql6 p别名在tabref只对应为一个表的别名。
//                    }
//                    break;
//                case HiveParser.TOK_TABLE_OR_COL:
//
//                    if (ast.getParent().getType() != HiveParser.DOT) {
//                        String col = ast.getChild(0).getText().toLowerCase();
//                        if (alias.get(col) == null
//                                && colAlais.get(nowQueryTable + "." + col) == null) {
//                            if (nowQueryTable.indexOf("&") > 0) {//sql23
//                                cols.put(UNKNOWN + "." + col, "");
//                            } else {
//                                cols.put(nowQueryTable + "." + col, "");
//                            }
//                        }
//                    }
//
//                    break;
//                case HiveParser.TOK_ALLCOLREF:
//                    if (ast.getChild(0) != null) {
//                        if (ast.getChild(0).getChild(0).toString() != null) {
//                            String bieming = ast.getChild(0).getChild(0).toString();
//                            //根据别名获取表名
//                            String s = alias.get(bieming);
//                            cols.put(s + ".*", "");
//                        } else {
//                            cols.put(nowQueryTable + ".*", "");
//                        }
//
//                    }
//
//                    break;
//                case HiveParser.TOK_SUBQUERY:
//                    if (ast.getChildCount() == 2) {
//                        String tableAlias = unescapeIdentifier(ast.getChild(1)
//                                .getText());
//                        String aliaReal = "";
//                        for (String table : set) {
//                            aliaReal += table + "&";
//                        }
//                        if (aliaReal.length() != 0) {
//                            aliaReal = aliaReal.substring(0, aliaReal.length() - 1);
//                        }
////                    alias.put(tableAlias, nowQueryTable);//sql22
//                        alias.put(tableAlias, aliaReal);//sql6
////                    alias.put(tableAlias, "");// just store alias
//                    }
//                    break;
//
//                case HiveParser.TOK_SELEXPR:
//                    if (ast.getChild(0).getType() == HiveParser.TOK_TABLE_OR_COL) {
//                        String column = ast.getChild(0).getChild(0).getText()
//                                .toLowerCase();
//                        if (nowQueryTable.indexOf("&") > 0) {
//                            cols.put(UNKNOWN + "." + column, "");
//                        } else if (colAlais.get(nowQueryTable + "." + column) == null) {
//                            cols.put(nowQueryTable + "." + column, "");
//                        }
//                    } else if (ast.getChild(1) != null) {// TOK_SELEXPR (+
//                        // (TOK_TABLE_OR_COL id)
//                        // 1) dd
//                        String columnAlia = ast.getChild(1).getText().toLowerCase();
//                        colAlais.put(nowQueryTable + "." + columnAlia, "");
//                    }
//                    break;
//
//
//                case HiveParser.DOT:
//                    if (ast.getType() == HiveParser.DOT) {
//                        if (ast.getChildCount() == 2) {
//                            if (ast.getChild(0).getType() == HiveParser.TOK_TABLE_OR_COL
//                                    && ast.getChild(0).getChildCount() == 1
//                                    && ast.getChild(1).getType() == HiveParser.Identifier) {
//                                String alia = BaseSemanticAnalyzer
//                                        .unescapeIdentifier(ast.getChild(0)
//                                                .getChild(0).getText()
//                                                .toLowerCase());
//                                String column = BaseSemanticAnalyzer
//                                        .unescapeIdentifier(ast.getChild(1)
//                                                .getText().toLowerCase());
//                                String realTable = null;
//                                if (!tables.contains(alia + "\t" + oper)
//                                        && alias.get(alia) == null) {
//                                    alias.put(alia, nowQueryTable);
//                                }
//                                if (tables.contains(alia + "\t" + oper)) {
//                                    realTable = alia;
//                                } else if (alias.get(alia) != null) {
//                                    realTable = alias.get(alia);
//                                }
//                                if (realTable == null || realTable.length() == 0 || realTable.indexOf("&") > 0) {
//                                    realTable = UNKNOWN;
//                                    //   realTable = nowQueryTable;
//                                }
//                                cols.put(realTable + "." + column, "");
//
//                            }
//                        }
//                    }
//                    break;
//
//                case HiveParser.TOK_ALTERTABLE:
//                    ASTNode alterTableName = (ASTNode) ast.getChild(0).getChild(0);
//
//                    String tName = alterTableName.getText();
//
//                    if (tName.split("\\.").length != 2) {
//                        tName = DEFAULT_DATABASES + "." + tName;
//                    }
//                    tables.add(tName + "\t" + oper);
//                    break;
//
//                case HiveParser.TOK_ALTERTABLE_ADDPARTS:
//
//
//                case HiveParser.TOK_ALTERTABLE_RENAME:
//                case HiveParser.TOK_ALTERTABLE_ADDCOLS:
////                    ASTNode alterTableName = (ASTNode) ast.getChild(0).getChild(0);
////
////
////                    tables.add(alterTableName.getText() + "\t" + oper);
////                   break;
//            }
//        }
//        return set;
//    }
//
//    private Set<String> parseChildNodes(ASTNode ast) {
//        Set<String> set = new HashSet<String>();
//        int numCh = ast.getChildCount();
//        if (numCh > 0) {
//            for (int num = 0; num < numCh; num++) {
//                ASTNode child = (ASTNode) ast.getChild(num);
//                set.addAll(parseIteral(child));
//            }
//        }
//        return set;
//    }
//
//    private void prepareToParseCurrentNodeAndChilds(ASTNode ast) {
//        if (ast.getToken() != null) {
//            switch (ast.getToken().getType()) {//join 从句开始
//                case HiveParser.TOK_RIGHTOUTERJOIN:
//                case HiveParser.TOK_LEFTOUTERJOIN:
//                case HiveParser.TOK_JOIN:
//                    joinClause = true;
//                    break;
//                case HiveParser.TOK_QUERY:
//                    tableNameStack.push(nowQueryTable);
//
//                    colAlaisStack.push(nowColAlais);
//                    operStack.push(oper);
//                    nowQueryTable = "";//sql22
//                    oper = Oper.SELECT;
//                    break;
//
//                case HiveParser.TOK_INSERT_INTO:
//                    tableNameStack.push(nowQueryTable);
//                    colAlaisStack.push(nowColAlais);
//                    operStack.push(oper);
//                    oper = Oper.INSERT_INTO;
//                    break;
//
//                case HiveParser.TOK_INSERT:
//                    tableNameStack.push(nowQueryTable);
//                    colAlaisStack.push(nowColAlais);
//                    operStack.push(oper);
//                    oper = Oper.INSERT;
//                    break;
//                case HiveParser.TOK_SELECT:
//                    tableNameStack.push(nowQueryTable);
//                    colAlaisStack.push(nowColAlais);
//                    operStack.push(oper);
//                    oper = Oper.SELECT;
//                    break;
//
//                case HiveParser.TOK_SUBQUERY_OP:
//                    isIn = true;
//                    break;
//
//
//                case HiveParser.TOK_DROPTABLE:
//                    oper = Oper.DROP;
//                    break;
//                case HiveParser.TOK_TRUNCATETABLE:
//                    oper = Oper.TRUNCATE;
//                    break;
//                case HiveParser.TOK_LOAD:
//                    oper = Oper.LOAD;
//                    break;
//                case HiveParser.TOK_CREATETABLE:
//                    oper = Oper.CREATETABLE;
//                    break;
//            }
//            if (ast.getToken() != null
//                    && ast.getToken().getType() >= HiveParser.TOK_ALTERDATABASE_PROPERTIES
//                    && ast.getToken().getType() <= HiveParser.TOK_ALTERVIEW_RENAME) {
////                tableNameStack.push(nowQueryTable);
////                operStack.push(oper);
//                oper = Oper.ALTER;
//            }
//        }
//    }
//
//    public static String unescapeIdentifier(String val) {
//        if (val == null) {
//            return null;
//        }
//        if (val.charAt(0) == '`' && val.charAt(val.length() - 1) == '`') {
//            val = val.substring(1, val.length() - 1);
//        }
//        return val;
//    }
//
//    private void output(Map<String, String> map) {
//        java.util.Iterator<String> it = map.keySet().iterator();
//        while (it.hasNext()) {
//            String key = it.next();
//            System.out.println(key + "\t" + map.get(key));
//        }
//    }
//
//    public void parsehive(ASTNode ast) {
//        parseIteral(ast);
//        System.out.println("***************表***************");
//        for (String table : tables) {
//
//            System.out.println(table);
//        }
//        System.out.println("***************列***************");
//        output(cols);
//        System.out.println("***************别名***************");
//        output(alias);
//    }
//
//    public Set<String> getTables() {
//        return tables;
//    }
//
//    public Map<String, String> getCols() {
//        return cols;
//    }
//
//    public static void main(String[] args) throws IOException, ParseException,
//            SemanticException {
//        ParseDriver pd = new ParseDriver();
//
//        String sql27 = "INSERT overwrite TABLE dw_my.forbidden_info\n" +
//                "select * from dw_my.forbidden_info where forbiddeninfo != 'ios_jinshanapple__508041831c9fe76b820902__exp_.'";
//        String sql28 = "INSERT INTO dc_bj_analysis.lyl_gcdmg_roledata  PARTITION (record_date='2017-03-22')\n" +
//                "select server_id,map_col[\"accoount_id\"] accoount_id,map_col[\"role_id\"] role_id,map_col[\"role_name\"] role_name,map_col[\"school\"] school,map_col[\"channelcode\"] channelcode,map_col[\"role_level\"] role_level,map_col[\"totalgametime\"] totalgametime,\n" +
//                "map_col[\"gold\"] gold,map_col[\"gold_b\"] gold_b,map_col[\"coin\"] coin,map_col[\"coin_b\"] coin_b,map_col[\"vip\"] vip,map_col[\"united\"] united,map_col[\"positin\"] positin,map_col[\"register_time\"] register_time,\n" +
//                "map_col[\"lastlogin_time\"] lastlogin_time\n" +
//                "from ods_log.role_bigtable_log \n" +
//                "where dt='2017-03-22' \n" +
//                "and game_id='xsj_gcd_cy' \n" +
//                "and table_name ='role_info'\n" +
//                "limit 100000";
//        String sql29 = "INSERT overwrite table ch_energy_item PARTITION (dt = :dt)\n" +
//                "SELECT ta.areaid, ta.roleid, ta.item, ta.increase, ta.cost, ta.remain\n" +
//                "FROM\n" +
//                "(SELECT ii.areaid,ii.roleid,ii.item,sum(if(ii.operation='add',ii.counts,0)) over (PARTITION by ii.areaid,ii.roleid) increase,\n" +
//                " sum(if(ii.operation='reduce',ii.counts,0)) over (PARTITION by ii.areaid,ii.roleid) cost, first_value(ii.remain) over (PARTITION by ii.areaid,ii.roleid order by ii.eventtime desc) remain\n" +
//                "FROM ch_itemflow ii \n" +
//                "WHERE ii.type = 10 and ii.dt =:dt)ta\n" +
//                "group by ta.areaid, ta.roleid, ta.item, ta.increase, ta.cost, ta.remain";
//
//
//        String sql111 = "\n" +
//                "insert overwrite table dc_bj.ods_xsj_jxqy_enhance_chardata\n" +
//                "PARTITION (dt)\n" +
//                "<#list 1..100 as num>\n" +
//                "  select  '${DateUtil.addDays('2016-10-16',num)}' -- 此处的时间为变量值，每次循环加1天\n" +
//                "  ,iZoneAreaID,vopenid,iRoleID,iEquipPos,max(iStrenLevel)iStrenLevel\n" +
//                "  FROM ods_jxqy_vn.ods_xsj_jxqy_EnhanceFlow a\n" +
//                "  where dt>'2016-10-16'\n" +
//                "  and dt<='${DateUtil.addDays('2016-10-16',num)}'  -- 此处的时间为变量值，每次循环加1天\n" +
//                "  group by '${DateUtil.addDays('2016-10-16',num)}',iZoneAreaID,vopenid,iRoleID,iEquipPos\n" +
//                "<#if num?has_next>\n" +
//                " union all \n" +
//                " </#if>\n" +
//                " </#list>\n" +
//                "\n" +
//                "\n" +
//                "\n";
//
//        Demo01 hp = new Demo01();
//        System.out.println(sql111);
//        ASTNode ast = pd.parse(sql111);
//        System.out.println(ast.toStringTree());
////        if (StringUtils.contains(ast.toStringTree(), "tok_subquery_op")) {
////            isIn = true;
////        }
//        hp.parsehive(ast);
//    }
//}