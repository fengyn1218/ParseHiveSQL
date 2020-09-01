package com.wanrennahan.testSQL.util;

import org.apache.hadoop.hive.ql.parse.ASTNode;
import org.apache.hadoop.hive.ql.parse.ParseDriver;
import org.apache.hadoop.hive.ql.parse.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @description: 文件处理工具类
 * @author: 冯雨南
 * @createDate: 2020/7/23
 * @version: 1.0.0
 */
public class FileUtils {
    /**
     * 以行为单位读取文件，常用于读面向行的格式化文件
     *
     * @return
     */
    public static StringBuilder[] readFileByLines(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        StringBuilder[] sqllists = new StringBuilder[100];
        StringBuilder sql = new StringBuilder();
        try {
            System.out.println("以字符单位读取文件内容：");
            reader = new BufferedReader(new FileReader(file));
            int temp;
            int line = 0;

            while ((temp = reader.read()) != -1) {

                if ((char) temp == ';') {
                    sqllists[line] = sql;
                    ++line;
                    sql = new StringBuilder();
                }

                sql.append((char) temp);


            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return sqllists;
    }

    public static void main(String[] args) throws ParseException {
        String parsesql = "INSERT overwrite table ch_energy_item PARTITION (dt = :dt)\n" +
                "SELECT ta.areaid, ta.roleid, ta.item, ta.increase, ta.cost, ta.remain\n" +
                "FROM\n" +
                "(SELECT ii.areaid,ii.roleid,ii.item,sum(if(ii.operation='add',ii.counts,0)) over (PARTITION by ii.areaid,ii.roleid) increase,\n" +
                " sum(if(ii.operation='reduce',ii.counts,0)) over (PARTITION by ii.areaid,ii.roleid) cost, first_value(ii.remain) over (PARTITION by ii.areaid,ii.roleid order by ii.eventtime desc) remain\n" +
                "FROM ch_itemflow ii \n" +
                "WHERE ii.type = 10 and ii.dt =:dt)ta\n" +
                "group by ta.areaid, ta.roleid, ta.item, ta.increase, ta.cost, ta.remain";
        ParseDriver parseDriver = new ParseDriver();
        ASTNode parse = parseDriver.parse(parsesql);
        System.out.println(parse);

    }
}
