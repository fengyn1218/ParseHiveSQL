package com.wanrennahan.testSQL.util;

import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.ParseDriver;
import org.apache.hadoop.hive.ql.parse.ParseException;

import java.util.HashSet;
import java.util.Set;

/**
 * @description: AST工具类
 * @author: YuNan.Feng
 * @createDate: 2020/8/1
 * @version: 1.0.0
 */
public class ASTUtils {

    /**
     * @description: 判断AST树是否“tok_tab”节点
     * @param: astNode
     * @return: boolean
     * @author: YuNan.Feng
     * @date: 2020/8/113:30
     * @version: 1.0.0
     **/
    public static boolean isContansTOKTAB(ASTNode astNode) {
        Set<String> set = ergodicAST(astNode);
        if (set.contains("925")&&set.contains("878")) {
            return true;
        }
        return false;
    }

    /**
     * @description: 遍历AST树所有节点
     * @param: astNode
     * @return: java.util.Set<java.lang.String>
     * @author: YuNan.Feng
     * @date: 2020/8/113:27
     * @version: 1.0.0
     **/
    private static Set<String> ergodicAST(ASTNode astNode) {
        Set<String> set = new HashSet<>();
        int numCh = astNode.getChildCount();

        if (numCh > 0) {
            for (int num = 0; num < numCh; num++) {
                ASTNode child = (ASTNode) astNode.getChild(num);
                set.add(child.getName());
                set.addAll(ergodicAST(child));

            }
        }
        return set;
    }

    public static void main(String[] args) throws ParseException {
        String str = "INSERT OVERWRITE LOCAL DIRECTORY '/data/rsync/TZ_GMT+08/2016-09-06/st_app_channel/continue_online/device_id/'\n" +
                "SELECT \n" +
                "'2016-09-06' tdate,'day' tdate_type,b.app_id,b.channel_id,b.server_id,\n" +
                "'continue_online' kpi_type,\n" +
                "count(distinct b.device_id)\n" +
                "FROM (\n" +
                "select \n" +
                "distinct app_id,device_id,account_id,channel_id,server_id\n" +
                "from dw_app a \n" +
                "WHERE a.tz='TZ_GMT+08' and a.dt >= '2016-09-06' AND a.dt <= '2016-09-06'\n" +
                ") a\n" +
                "INNER JOIN (\n" +
                "SELECT \n" +
                "distinct app_id,device_id,account_id,channel_id,server_id\n" +
                "FROM dw_app\n" +
                "WHERE tz='TZ_GMT+08' and \n" +
                "dt >= '2016-09-05' AND dt <= '2016-09-05'\n" +
                ") b ON a.device_id = b.device_id AND a.app_id = b.app_id\n" +
                "GROUP BY b.app_id,b.channel_id, b.server_id WITH CUBE";
        ASTNode parse = new ParseDriver().parse(str);
        System.out.println(parse.toStringTree());
        boolean contansTOKTAB = isContansTOKTAB(parse);
        System.out.println(contansTOKTAB);
    }

}
