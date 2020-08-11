package com.xishanju.testSQL.fildname;

import com.xishanju.testSQL.util.FileUtils;
import org.apache.hadoop.hive.ql.parse.*;

import java.io.IOException;
import java.util.*;
/**
 * 目的：获取AST中的表，列，以及对其所做的操作，如SELECT,INSERT
 * 重点：获取SELECT操作中的表和列的相关操作。其他操作这判断到表级别。
 * 实现思路：对AST深度优先遍历，遇到操作的token则判断当前的操作，
 *                     遇到TOK_TAB或TOK_TABREF则判断出当前操作的表，遇到子句则压栈当前处理，处理子句。
 *                    子句处理完，栈弹出。
 *
 */
public class HiveParse {

    private  static final String UNKNOWN = "UNKNOWN";
    private Map<String, String> alias = new HashMap<String, String>();
    private Map<String, String> cols = new TreeMap<String, String>();
    private Map<String, String> colAlais = new TreeMap<String, String>();
    private Set<String> tables = new HashSet<String>();
    private Stack<String> tableNameStack = new Stack<String>();
    private Stack<Oper> operStack = new Stack<Oper>();
    private String nowQueryTable = "";//定义及处理不清晰，修改为query或from节点对应的table集合或许好点。目前正在查询处理的表可能不止一个。
    private Oper oper ;
    private boolean joinClause = false;

    private enum Oper {
        SELECT, INSERT, DROP, TRUNCATE, LOAD, CREATETABLE, ALTER
    }
    public Set<String> parseIteral(ASTNode ast) {
        Set<String> set= new HashSet<String>();//当前查询所对应到的表集合
        prepareToParseCurrentNodeAndChilds(ast);
        set.addAll(parseChildNodes(ast));
        set.addAll(parseCurrentNode(ast ,set));
        endParseCurrentNode(ast);
        return set;
    }
    private void endParseCurrentNode(ASTNode ast){
        if (ast.getToken() != null) {
            switch (ast.getToken().getType()) {//join 从句结束，跳出join
                case HiveParser.TOK_RIGHTOUTERJOIN:
                case HiveParser.TOK_LEFTOUTERJOIN:
                case HiveParser.TOK_JOIN:
                    joinClause = false;
                    break;
                case HiveParser.TOK_QUERY:
                    break;
                case HiveParser.TOK_INSERT:
                case HiveParser.TOK_SELECT:
                    nowQueryTable = tableNameStack.pop();
                    oper = operStack.pop();
                    break;
            }
        }
    }
    private Set<String> parseCurrentNode(ASTNode ast, Set<String> set){
        if (ast.getToken() != null) {
            switch (ast.getToken().getType()) {
                case HiveParser.TOK_TABLE_PARTITION:
//            case HiveParser.TOK_TABNAME:
                    if (ast.getChildCount() != 2) {
                        String table = BaseSemanticAnalyzer
                                .getUnescapedName((ASTNode) ast.getChild(0));
                        if (oper == Oper.SELECT) {
                            nowQueryTable = table;
                        }
                        tables.add(table + "\t" + oper);
                    }
                    break;

                case HiveParser.TOK_TAB:// outputTable
                    String tableTab = BaseSemanticAnalyzer
                            .getUnescapedName((ASTNode) ast.getChild(0));
                    if (oper == Oper.SELECT) {
                        nowQueryTable = tableTab;
                    }
                    tables.add(tableTab + "\t" + oper);
                    break;
                case HiveParser.TOK_TABREF:// inputTable
                    ASTNode tabTree = (ASTNode) ast.getChild(0);
                    String tableName = (tabTree.getChildCount() == 1) ? BaseSemanticAnalyzer
                            .getUnescapedName((ASTNode) tabTree.getChild(0))
                            : BaseSemanticAnalyzer
                            .getUnescapedName((ASTNode) tabTree.getChild(0))
                            + "." + tabTree.getChild(1);
                    if (oper == Oper.SELECT) {
                        if(joinClause && !"".equals(nowQueryTable) ){
                            nowQueryTable += "&"+tableName;//
                        }else{
                            nowQueryTable = tableName;
                        }
                        set.add(tableName);
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
                        if (alias.get(col) == null
                                && colAlais.get(nowQueryTable + "." + col) == null) {
                            if(nowQueryTable.indexOf("&") > 0){//sql23
                                cols.put(UNKNOWN + "." + col, "");
                            }else{
                                cols.put(nowQueryTable + "." + col, "");
                            }
                        }
                    }
                    break;
                case HiveParser.TOK_ALLCOLREF:
                    cols.put(nowQueryTable + ".*", "");
                    break;
                case HiveParser.TOK_SUBQUERY:
                    if (ast.getChildCount() == 2) {
                        String tableAlias = unescapeIdentifier(ast.getChild(1)
                                .getText());
                        String aliaReal = "";
                        for(String table : set){
                            aliaReal+=table+"&";
                        }
                        if(aliaReal.length() !=0){
                            aliaReal = aliaReal.substring(0, aliaReal.length()-1);
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
                        if(nowQueryTable.indexOf("&") > 0){
                            cols.put(UNKNOWN + "." + column, "");
                        }else if (colAlais.get(nowQueryTable + "." + column) == null) {
                            cols.put(nowQueryTable + "." + column, "");
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
                                        && alias.get(alia) == null) {// [b SELECT, a
                                    // SELECT]
                                    alias.put(alia, nowQueryTable);
                                }
                                if (tables.contains(alia + "\t" + oper)) {
                                    realTable = alia;
                                } else if (alias.get(alia) != null) {
                                    realTable = alias.get(alia);
                                }
                                if (realTable == null || realTable.length() == 0 || realTable.indexOf("&") > 0) {
                                    realTable = UNKNOWN;
                                }
                                cols.put(realTable + "." + column, "");

                            }
                        }
                    }
                    break;
                case HiveParser.TOK_ALTERTABLE_ADDPARTS:
                case HiveParser.TOK_ALTERTABLE_RENAME:
                case HiveParser.TOK_ALTERTABLE_ADDCOLS:
                    ASTNode alterTableName = (ASTNode) ast.getChild(0);
                    tables.add(alterTableName.getText() + "\t" + oper);
                    break;
            }
        }
        return set;
    }
    private  Set<String> parseChildNodes(ASTNode ast){
        Set<String> set= new HashSet<String>();
        int numCh = ast.getChildCount();
        if (numCh > 0) {
            for (int num = 0; num < numCh; num++) {
                ASTNode child = (ASTNode) ast.getChild(num);
                set.addAll(parseIteral(child));
            }
        }
        return set;
    }
    private void prepareToParseCurrentNodeAndChilds(ASTNode ast){
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
                case HiveParser.TOK_INSERT:
                    tableNameStack.push(nowQueryTable);
                    operStack.push(oper);
                    oper = Oper.INSERT;
                    break;
                case HiveParser.TOK_SELECT:
                    tableNameStack.push(nowQueryTable);
                    operStack.push(oper);
//                    nowQueryTable = nowQueryTable
                    // nowQueryTable = "";//语法树join
                    // 注释语法树sql9， 语法树join对应的设置为""的注释逻辑不符
                    oper = Oper.SELECT;
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
        java.util.Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            System.out.println(key + "\t" + map.get(key));
        }
    }
    public void parse(ASTNode ast) {
        parseIteral(ast);
        System.out.println("***************表***************");
        for (String table : tables) {
            System.out.println(table);
        }
        System.out.println("***************列***************");
        output(cols);
        System.out.println("***************别名***************");
        output(alias);
    }
    public static void main(String[] args) throws IOException, ParseException,
            SemanticException {
        ParseDriver pd = new ParseDriver();
        // HiveConf conf = new HiveConf();
        String sql1 = "Select * from zpc1";
        String sql2 = "Select name,ip from zpc2 bieming where age > 10 and area in (select area from city)";
        String sql3 = "Select d.name,d.ip from (select * from zpc3 where age > 10 and area in (select area from city)) d";
        String sql4 = "create table zpc(id string, name string)";
        String sql5 = "insert overwrite table tmp1 PARTITION (partitionkey='2008-08-15') select * from tmp";
        String sql6 = "FROM (  SELECT p.datekey datekey, p.userid userid, c.clienttype  FROM detail.usersequence_client c JOIN fact.orderpayment p ON p.orderid = c.orderid "
                + " JOIN default.user du ON du.userid = p.userid WHERE p.datekey = 20131118 ) base  INSERT OVERWRITE TABLE `test`.`customer_kpi` SELECT base.datekey, "
                + "  base.clienttype, count(distinct base.userid) buyer_count GROUP BY base.datekey, base.clienttype";
        String sql7 = "SELECT id, value FROM (SELECT id, value FROM p1 UNION ALL  SELECT 4 AS id, 5 AS value FROM p1 limit 1) u";
        String sql8 = "select dd from(select id+1 dd from zpc) d";
        String sql9 = "select dd+1 from(select id+1 dd from zpc) d";
        String sql10 = "truncate table zpc";
        String sql11 = "drop table zpc";
        String sql12 = "select * from tablename where unix_timestamp(cz_time) > unix_timestamp('2050-12-31 15:32:28')";
        String sql15 = "alter table old_table_name RENAME TO new_table_name";
        String sql16 = "select statis_date,time_interval,gds_cd,gds_nm,sale_cnt,discount_amt,discount_rate,price,etl_time,pay_amt from o2ostore.tdm_gds_monitor_rt where time_interval = from_unixtime(unix_timestamp(concat(regexp_replace(from_unixtime(unix_timestamp('201506181700', 'yyyyMMddHHmm')+ 84600 ,  'yyyy-MM-dd HH:mm'),'-| |:',''),'00'),'yyyyMMddHHmmss'),'yyyy-MM-dd HH:mm:ss')";
        String sql13 = "INSERT OVERWRITE TABLE u_data_new SELECT TRANSFORM (userid, movieid, rating, unixtime) USING 'python weekday_mapper.py' AS (userid, movieid, rating, weekday) FROM u_data";
        String sql14 = "SELECT a.* FROM a JOIN b ON (a.id = b.id AND a.department = b.department)";
        String sql17 = "LOAD DATA LOCAL INPATH \"/opt/data/1.txt\" OVERWRITE INTO TABLE table1";
        String sql18 = "CREATE TABLE  table1     (    column1 STRING COMMENT 'comment1',    column2 INT COMMENT 'comment2'        )";
        String sql19 = "ALTER TABLE events RENAME TO 3koobecaf";
        String sql20 = "ALTER TABLE invites ADD COLUMNS (new_col2 INT COMMENT 'a comment')";
        String sql21 = "alter table mp add partition (b='1', c='1')";
        String sql22 = "select login.uid from login day_login left outer join (select uid from regusers where dt='20130101') day_regusers on day_login.uid=day_regusers.uid where day_login.dt='20130101' and day_regusers.uid is null";
        String sql23 = "select name from (select * from zpc left outer join def) d";
        StringBuilder[] sqllists = FileUtils.readFileByLines("C:/Users/Kingsoft/Desktop/sql.txt");
        String parsesql = sqllists[0].toString();
        HiveParse hp = new HiveParse();
        System.out.println(parsesql);
        ASTNode ast = pd.parse(parsesql);
        System.out.println(ast.toStringTree());
        hp.parse(ast);
    }
}