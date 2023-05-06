package com.action.archives.util;

import weaver.conn.RecordSet;

/**
 * @author Li Yu Feng
 * @date 2023-03-10 14:02
 */
public class ArchivesUtil {
    private ArchivesUtil(){

    }
    public static String getParameterValue(String key, RecordSet rs) {
        rs.executeQuery("select PROPVALUE from uf_properties  where propname=?", key);
        if (rs.next()) {
            return rs.getString("PROPVALUE") == null ? "" : rs.getString("PROPVALUE");
        }
        return "";
    }
}
