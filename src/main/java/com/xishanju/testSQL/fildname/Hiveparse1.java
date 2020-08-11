package com.xishanju.testSQL.fildname;

import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.QueryState;
import org.apache.hadoop.hive.ql.parse.*;
import org.apache.hadoop.hive.ql.session.SessionState;


/**
 * @description:
 * @author: 冯雨南
 * @createDate: 2020/7/21
 * @version: 1.0.0
 */
public class Hiveparse1 {


    public static void main(String[] args) throws Exception {

        HiveConf conf = new HiveConf(Driver.class);
        conf.setVar(HiveConf.ConfVars.HIVE_AUTHORIZATION_MANAGER,
                "org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdHiveAuthorizerFactory");
        HiveConf.setBoolVar(conf, HiveConf.ConfVars.HIVE_SUPPORT_CONCURRENCY, false);
        conf.setBoolVar(HiveConf.ConfVars.HIVE_STATS_COLLECT_SCANCOLS, true);
        SessionState.start(conf);

        QB qb = new QB();

        String sql = "select * from test.test1";
//        Driver driver = createDriver();
        QueryState queryState = new QueryState(conf);
        ParseDriver parseDriver = new ParseDriver();
        ASTNode astNode = parseDriver.parse(sql);


        BaseSemanticAnalyzer analyzer = SemanticAnalyzerFactory.get(queryState, astNode);
//        SemanticAnalyzer analyzer1=new SemanticAnalyzer(queryState);
        SemanticAnalyzer semanticAnalyzer = (SemanticAnalyzer) analyzer;
        semanticAnalyzer.doPhase1(astNode, qb, null, null);


    }

    static class Phase1Ctx {
        String dest;
        int nextNum;
    }


    static class PlannerContext {
        protected ASTNode child;
        protected Phase1Ctx ctx_1;

        void setParseTreeAttr(ASTNode child, Phase1Ctx ctx_1) {
            this.child = child;
            this.ctx_1 = ctx_1;
        }

        void setCTASToken(ASTNode child) {
        }

        void setInsertToken(ASTNode ast, boolean isTmpFileDest) {
        }

    }

    private static org.apache.hadoop.hive.ql.Driver createDriver() {
        HiveConf conf = new HiveConf(org.apache.hadoop.hive.ql.Driver.class);
        conf
                .setVar(HiveConf.ConfVars.HIVE_AUTHORIZATION_MANAGER,
                        "org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdHiveAuthorizerFactory");
        HiveConf.setBoolVar(conf, HiveConf.ConfVars.HIVE_SUPPORT_CONCURRENCY, false);
        conf.setBoolVar(HiveConf.ConfVars.HIVE_STATS_COLLECT_SCANCOLS, true);
        SessionState.start(conf);
        org.apache.hadoop.hive.ql.Driver driver = new org.apache.hadoop.hive.ql.Driver(conf);
        driver.init();
        return driver;
    }
}

//    public void shouldAnswerWithTrue() throws IOException, SemanticException {
//        LineageLogger logger = new LineageLogger();
//
//        HiveConf hiveConf = new HiveConf();
//
//
//        hiveConf.set("javax.jdo.option.ConnectionURL", "jdbc:mysql://localhost/metastore", "hive-conf.xml");
//        hiveConf.set("javax.jdo.option.ConnectionDriverName", "com.mysql.jdbc.Driver", "hive-conf.xml");
//        hiveConf.set("javax.jdo.option.ConnectionUserName", "root", "hive-conf.xml");
//        hiveConf.set("javax.jdo.option.ConnectionPassword", "moye", "hive-conf.xml");
//        hiveConf.set("fs.defaultFS", "hdfs://127.0.0.1:8020", "hdfs-site.xml");
//        hiveConf.set("_hive.hdfs.session.path", "hdfs://127.0.0.1:8020/tmp", "hive-conf.xml");
//        hiveConf.set("_hive.local.session.path", "hdfs://127.0.0.1:8020/tmp", "hive-conf.xml");
//        hiveConf.set("hive.in.test", "true", "hive-conf.xml");
//
//
//        String sql = "insert overwrite table sucx.test  select * from sucx.test2";
//        QueryState queryState = new QueryState(hiveConf);
//
//        Context context = new Context(hiveConf);
//
//        SessionState sessionState = new SessionState(hiveConf);
//
//        SessionState.setCurrentSessionState(sessionState);
//
//        ASTNode astNode = ParseUtils.parse(sql, context);
//
//        BaseSemanticAnalyzer analyzer = SemanticAnalyzerFactory.get(queryState, astNode);
//
//        analyzer.analyze(astNode, context);
//
//        Schema schema = Reflect.onClass(Driver.class).call("getSchema", analyzer, hiveConf).get();
//
//        QueryPlan queryPlan = new QueryPlan(sql, analyzer, 0L, null, queryState.getHiveOperation(), schema);
//
//
//
//        HookContext hookContext = new HookContext(queryPlan, queryState,
//                new HashMap<>(), "sucx", "",
//                "", "", "", "",
//                true, null);
//
//        hookContext.setUgi(UserGroupInformation.getCurrentUser());
//        logger.run(hookContext);
//    }
//}
