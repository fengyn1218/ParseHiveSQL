package com.wanrennahan.testSQL.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @description: 字符串工具类
 * @author: 冯雨南
 * @createDate: 2020/7/27
 * @version: 1.0.0
 */
public class StringUtils {

    /**
     * @description: 判断一个字符串是否以Set、Create、use开头,排除注释语句，freemarker语句
     * @param: s
     * @return: boolean
     * @author: YuNan.Feng
     * @date: 2020/7/27 9:15
     * @version: 1.0.0
     **/
    public static boolean isStringContainOtherKey(String s) {
        String trim = s.trim().toLowerCase();
        if (trim.startsWith("set") || trim.startsWith("create") || trim.startsWith("use") || trim.startsWith("from") || trim.startsWith("alter") || trim.contains("<#") || trim.contains("</#") || trim.startsWith("drop") || trim.startsWith("load") || !trim.contains("select") || !trim.contains("from")) {
            return true;
        }
        return false;
    }

    /**
     * @description: 处理带注释的语句(分割前处理)
     * @param: str
     * @return: java.lang.String
     * @author: YuNan.Feng
     * @date: 2020/7/29 14:26
     * @version: 1.0.0
     **/
    public static String handleComment(String str) {
        String trim = str.trim();
        String strCmd;
        String result;
        int index = trim.indexOf("\n");
        String substring = trim.substring(0, index + 1);
        strCmd = str.replace(substring, "");
        if (strCmd.trim().startsWith("--")) {
            result = handleComment(strCmd);
        } else {
            result = strCmd;
        }
        return result;

    }

    /**
     * @description: 处理字段后面跟的注释
     * @param: string
     * @return: java.lang.String
     * @author: YuNan.Feng
     * @date: 2020/7/31 14:24
     * @version: 1.0.0
     **/
    public static String handleFieldComment(String string) {
        int i = string.indexOf("--");
        String substring1 = string.substring(i);
        int j = substring1.indexOf("\n");
        String substring = string.substring(i, i + j);
        String result = string.replace(substring, "");
        if (result.contains("--")) {
            result = handleFieldComment(result);
        }
        return result;
    }

    /**
     * @description: 处理结果字段的符号
     * @param: stringField
     * @return: java.lang.String
     * @author: YuNan.Feng
     * @date: 2020/7/31 14:25
     * @version: 1.0.0
     **/
    public static String handleField(String stringField) {
        String s = stringField.replaceAll("[^a-zA-Z0-9._*]", " ");
        if (s.trim() == null || s.trim().equals("")) {
            return null;
        }
        String[] s1 = s.split(" ");
        return s1[s1.length - 1];
    }

    /**
     * @description: 将字符串分割，排除无用关键字语句后返回
     * @param: ss
     * @return: java.util.List<java.lang.String>
     * @author: YuNan.Feng
     * @date: 2020/7/27 9:34
     * @version: 1.0.0
     **/
    public static List<String> string2List(String ss) {
        List<String> list = new ArrayList<>();
        //处理语句前注释
        if (ss.trim().startsWith("--")) {
            ss = handleComment(ss);
        }
        String[] split = ss.trim().split(";");
        String tempCmd = "";
        String tempCmd1 = "";
        for (int i = 0; i < split.length; i++) {
            String temp = split[i];
            String command;
            if (temp != null && temp.length() != 0 && !onlyContains(temp)) {
                if (!isStringContainOtherKey(temp)) {
                    if (isContainParam(temp)) {
                        temp = temp.replace(":dt", "'2020'");
                    }
                    //处理语句间的注释
                    if (temp.trim().startsWith("--")) {
                        tempCmd1 += temp + "\n";
                        if (i + 1 >= split.length) {
                            list.add(tempCmd1);
                        }
                        continue;
                    } else {
                        command = tempCmd1 + temp;
                        tempCmd1 = "";
                    }
                    //处理正则表达式的分号
                    if (temp.endsWith("\\")) {
                        tempCmd += org.apache.commons.lang.StringUtils.chop(temp) + ";";
                        continue;
                    } else {
                        command = tempCmd + temp;
                        tempCmd = "";
                    }
                    list.add(command);
                }
            }
        }
        return list;
    }

    /**
     * @description: 判断字符是否包含自定义变量
     * @param: sss
     * @return:
     * @author: YuNan.Feng
     * @date: 2020/7/27 10:20
     * @version: 1.0.0
     **/
    private static boolean isContainParam(String sss) {
        if (org.apache.commons.lang3.StringUtils.contains(sss, ":dt")) {
            return true;
        }
        return false;
    }

    /**
     * @description: 判断字符串是否只包含 \n \t
     * @param: string
     * @return: boolean
     * @author: YuNan.Feng
     * @date: 2020/7/27 14:44
     * @version: 1.0.0
     **/
    private static boolean onlyContains(String string) {
        String s = string.replace("\n", "");
        if (s.trim().length() > 0) {
            return false;
        }
        String s1 = string.replace("\t", "");
        if (s1.trim().length() > 0) {
            return false;
        }
        return true;
    }

    /**
     * @description: 判断一个字符串是否为数字
     * @param: str
     * @return: boolean
     * @author: YuNan.Feng
     * @date: 2020/8/1 17:50
     * @version: 1.0.0
     **/
    public static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }
}
