package com.api.archives.web;

import com.action.archives.server.ArchivePreArchiveServer;
import com.alibaba.fastjson.JSON;
import weaver.conn.RecordSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Li Yu Feng
 * @date 2023-03-10 13:46
 */
@Path("/archives")
public class ProfileResultUpdateWeb {

    @POST
    @Path("/updateArchivesInfo")
    @Produces(MediaType.TEXT_PLAIN)
    public String theArchiveReceivesTheResults(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        return new ArchivePreArchiveServer().updateArchivesInfo(request);
    }
    @GET
    @Path("/anewPushArchives")
    @Produces(MediaType.TEXT_PLAIN)
    public String anewPushArchives(@Context HttpServletRequest request, @Context HttpServletResponse response) {
        String ids = request.getParameter("ids");
        String[] spIds = ids.split(",");
        Map<String,String> map=new HashMap<>();

        if (spIds.length==0){
            map.put("code","201");
            map.put("msg","id不能为空");
            return JSON.toJSONString(map);
        }
        RecordSet rs = new RecordSet();
        for (int i = 0; i < spIds.length; i++) {
            rs.executeQuery("select lcmc from uf_ygdjl where id=?",spIds[i]);
            if (rs.next()){
                if (rs.getInt("lcmc")>0){
                    ArchivePreArchiveServer server = new ArchivePreArchiveServer();
                    Map<String, String> requestArchive = server.sendRequestArchive(rs.getInt("lcmc"));
                    /*if (requestArchive.get("code").equals("200")){
                        map.put(String.valueOf(rs.getInt("lcmc")),requestArchive.get("code"));
                    }else {
                        map.put(String.valueOf(rs.getInt("lcmc")),requestArchive.get("msg"));
                    }*/

                    return JSON.toJSONString(requestArchive);
                } else {
                    map.put("code","201");
                    map.put("msg","ruquestid不能为空");
                    return JSON.toJSONString(map);
                }
            }

        }
        return JSON.toJSONString(map);
    }
}
