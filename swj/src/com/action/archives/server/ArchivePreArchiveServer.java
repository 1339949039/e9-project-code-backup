package com.action.archives.server;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import com.action.archives.util.ArchivesUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.api.formmode.page.util.Util;
import com.engine.common.util.ServiceUtil;
import com.engine.workflow.service.HtmlToPdfService;
import com.engine.workflow.service.impl.HtmlToPdfServiceImpl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weaver.common.StringUtil;
import weaver.conn.RecordSet;
import weaver.conn.RecordSetData;
import weaver.file.ImageFileManager;
import weaver.formmode.setup.ModeRightInfo;
import weaver.general.GCONST;
import weaver.hrm.User;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author Li Yu Feng
 * @date 2023-03-13 17:46
 */
public class ArchivePreArchiveServer {
    private Logger logger = LoggerFactory.getLogger("lyflog");

    private String workflowName = "";
    private String wfTablename = "formtable_main_";
    private String workflowid = "";
    private String requestName = "";
    private int requestId = 0;
    private int stepcode = 0;
    private String docNo = "";
    private String dataName = "";//文件名称
    private int tszt = 1;//推送预归档系统状态
    private String gdsj = "";//归档时间
    private String sbyy = "";//推送预归档失败原因
    private String result = "";//档案系统检测结果
    private String detection = "";//档案系统检测描述
    private User user;//流程创建人
    private String typeName = "发文";
    private String nodeName="主办处室处长";

    private String path = GCONST.getRootPath() + "filesystem" + File.separator + "hnfileTemp";//临时文件存放位置

    public Map<String, String> sendRequestArchive(int requestId) {

        Map<String, String> resultMap = new HashMap<>();
        long start = System.currentTimeMillis();
        logger.info("档案预归档集成: start");
        this.requestId = requestId;
        logger.info("requestid = " + this.requestId);
        RecordSet rs = new RecordSet();
        List<Map<String, String>> files = new ArrayList<>();

        try {
            //获取WORKFLOWID，REQUESTNAME，creater并赋值
            rs.executeQuery("select WORKFLOWID,REQUESTNAME,creater from workflow_requestbase  where requestid=?", requestId);
            if (rs.next()) {
                workflowid = rs.getString("WORKFLOWID");
                requestName = rs.getString("REQUESTNAME");
                user = new User(rs.getInt("creater"));
            }
            logger.info("workflowid = " + workflowid);
            //获取wfTablename，REQUESTNAME，creater并赋值
            rs.executeQuery("select formid,WORKFLOWNAME from workflow_base where id=?", workflowid);
            if (rs.next()) {
                wfTablename += Math.abs(rs.getInt("formid"));
                workflowName = rs.getString("WORKFLOWNAME");
            }
            logger.info("workflowName = " + workflowName);

            //外部收文：收文 对外发文、内部呈批：发文
            if (workflowName.contains("收文")||workflowName.contains("处室办件")) {
                typeName = "收文";
                nodeName="主办处室处长分办";
            }
            logger.info("wfTablename = " + wfTablename);
            logger.info("requestName = " + requestName);

            Map<String, String> mainTableMap = new HashMap<>();//主表数据
            rs.executeQuery("SELECT * FROM " + wfTablename + "  where requestid=" + this.requestId);
            if (rs.next()) {
                //  获取源数据
                RecordSetData metaData = rs.getData();
                //获取列的个数
                int columnCount = metaData.getColCounts();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i).toLowerCase();
                    mainTableMap.put(columnName, rs.getString(i) == null ? "" : rs.getString(i));
                }
            }
            logger.info("主表数据 = " + JSON.toJSONString(mainTableMap));
            //1.拿workflowid去找建模表的映射字段 建模表 uf_parameterMapping
            rs.executeQuery("SELECT * FROM uf_parameterMapping where process=?", workflowid);
            Map<String, String> oneDataMap = new HashMap<>();
            if (rs.next()) {
                //  获取源数据
                RecordSetData metaData = rs.getData();
                //获取列的个数
                int columnCount = metaData.getColCounts();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i).toLowerCase();
                    oneDataMap.put(columnName, rs.getString(i) == null ? "" : rs.getString(i));
                }
            } else {
                throw new Exception("没有配置档案映射表");
            }
            //拿到映射表的映射字段
            logger.info("建模表映射数据oneDataMap = " + JSON.toJSONString(oneDataMap));
            //获取正文附件和指定附件字段
            String zwid = mainTableMap.get("zw") == null ? "" : mainTableMap.get("zw");
            String filewjzd = getValue(mainTableMap, oneDataMap.get("filewjzd"));
            List<Map<String, String>> zwfile = new ArrayList<>();
            if (StringUtil.isNotNull(zwid)) {
                zwfile = getFiles(zwid, rs, "正本");
            }
            List<Map<String, String>> fjfiles = new ArrayList<>();

            if (StringUtil.isNotNull(filewjzd)) {
                fjfiles = getFiles(filewjzd, rs, "相关附件");
            }
            //生成pdf文件
            //formPdf = {"pathNew":"文件处理表.pdf","pdffilename":"日常报销流程-系统管理员-2022-05-13_20220623162814_pdf.pdf","path":"D:/testpdf","htmlfilename":"日常报销流程-系统管理员-2022-05-13_20220623162814.zip"}
            Map<String, Object> formPdf = getFormPdf(this.user, String.valueOf(requestId), "", this.path);
            Map<String, String> formPdfNew = new HashMap<>();
            formPdfNew.put("disname", String.valueOf(formPdf.get("disname")));//母亲身份证.txt
            formPdfNew.put("operatedatetime", String.valueOf(formPdf.get("operatedatetime")));
            formPdfNew.put("filepath", String.valueOf(formPdf.get("filepath")));//D:/ddm/yzm.jpg
            formPdfNew.put("md5", "");//123dwe21321ewq123
            formPdfNew.put("filetype", String.valueOf(formPdf.get("filetype")));//母亲身份证
            formPdfNew.put("filesize", String.valueOf(formPdf.get("filesize")));//文件大小


            files.addAll(zwfile);
            files.addAll(fjfiles);
            files.add(formPdfNew);

            logger.info("files = " + JSON.toJSONString(files));
            List<File> fileDatas = new ArrayList<>();
            int serialNumber = 2;
            for (Map<String, String> file : files) {
                File file1 = new File(file.get("filepath"));
                if (StringUtil.isNotNull(dataName)) {
                    dataName += "," + file.get("disname");
                } else {
                    dataName = file.get("disname");
                }
                int dotIndex = file.get("disname").lastIndexOf(".");
                String fileExtension = "";
                if (dotIndex > 0) {
                    fileExtension = file.get("disname").substring(dotIndex + 1);
                }


                file.put("fileformat", fileExtension);//文件类型pdf,ofd，wps
                file.put("filedate", file.get("operatedatetime"));//文件上传系统时间
                //file.put("fileprocedure", "");//WPS/odf名称版本
                //"fileprocedure": "软件环境","fileattributes":"计算机文件属性","hardwareprocedure":"硬件环境"
                file.put("fileprocedure", "");//软件环境
                file.put("fileattributes", "");//计算机文件属性
                file.put("hardwareprocedure", "");//硬件环境
                file.put("manuscriptformat", fileExtension);//文件第一次上传系统的格式
                //  收文按收文处理表、正本、相关附件排列，
                //  发文按正本、发文处理表、定稿（包括法律法规等重要文件的历次修改稿）、相关附件排列。
                String order = "";
                //收文：深圳市水务局文件处理表

                //发文：深圳市水务局文件呈批表 (发文)
                //内部呈批：深圳市水务局文件处理表 (内部呈批)

                //外部收文：收文 对外发文、内部呈批：发文
                if (typeName.equals("收文")) {
                    if (file.get("filetype").contains("收文处理表")) {
                        order = "1";
                    } else if (file.get("filetype").contains("正本")) {
                        order = "2";
                    } else if (file.get("filetype").contains("相关附件")) {
                        serialNumber++;
                        order = String.valueOf(serialNumber);
                    }
                } else {
                    if (file.get("filetype").contains("正本")) {
                        order = "1";
                    } else if (file.get("filetype").contains("发文处理表")) {
                        order = "2";
                    } else if (file.get("filetype").contains("相关附件")) {
                        serialNumber++;
                        order = String.valueOf(serialNumber);
                    }
                }
                file.put("fileorder", order);
                file.put("filemanuscript", file.get("filetype"));//草稿、定稿、正本、副本、试行本、修订本、附件、处置单、[其他]
                fileDatas.add(file1);
            }
            //转成数组
            File[] fileArray = fileDatas.toArray(new File[0]);

            //拿映射字段获取表单的值
            Map<String, Object> mapTheResults = mapTheResults(mainTableMap, oneDataMap, rs, files);


            String jsonData = JSON.toJSONString(mapTheResults);
            logger.info("建模表映射主表数据jsonData = " + jsonData);


            //随机生成4位数字
            String x_nonce = String.valueOf((new Random().nextInt(9000) + 1000));
            //生成时间戳
            String x_timestamp = String.valueOf(Instant.now().getEpochSecond());
            //获取动态参数
            String x_apiKey = ArchivesUtil.getParameterValue("archives_x_apiKey", rs);//3d6c7fa54d694e419ae71d7e80940c58
            String x_sign = ArchivesUtil.getParameterValue("archives_x_sign", rs);//1dcab2b01ccef43bed708c7bd1df4c9e
            String url = ArchivesUtil.getParameterValue("archives_url", rs);//http://3a8b0084.r8.cpolar.top/tyarchive/preGd/savePreData
            //发送数据到档案系统
            //http://3a8b0084.r8.cpolar.top/tyarchive/preGd/savePreData
            String result = HttpRequest.post(url)
                    .header("x-apiKey", x_apiKey)//"3d6c7fa54d694e419ae71d7e80940c58"
                    .header("x-sign", x_sign)//1dcab2b01ccef43bed708c7bd1df4c9e
                    .header("x-nonce", x_nonce)
                    .header("x-timestamp", x_timestamp)
                    .form("data", jsonData)
                    .form("files", fileArray)
                    .execute().body();
            logger.info("发送档案result:" + result);
            tszt = 0;//已推送
            JSONObject obj = JSON.parseObject(result);
            String code = Convert.toStr(obj.get("code"));
            if (!"10000".equals(code)) {
                throw new Exception(StrUtil.hasEmpty(Convert.toStr(obj.get("msg"))) == true ? "未知异常" : Convert.toStr(obj.get("msg")));
            }


            //actionResult.success();
            resultMap.put("code", "200");
        } catch (Exception e) {
            tszt = 2;//推送失败
            sbyy = e.getMessage();
            logger.error("档案推送异常:", e);
            resultMap.put("code", "201");
            resultMap.put("msg", e.getMessage());
            //actionResult.failure(e.getMessage());
        } finally {

            String formmodeid = "";
            rs.executeQuery("select id from modeinfo where formid = (select id from workflow_bill where tablename='uf_ygdjl')");
            if (rs.next()) {
                formmodeid = rs.getString("id");
            }

            /**
             *
             * docNo 办文编号	lcmc  流程名称      tszt   推送预归档系统状态 gdsj   归档时间
             * sbyy     推送预归档失败原因    dataName   文件名称 result 档案系统检测结果
             * detection    档案系统检测描述
             */
            gdsj = DateUtil.now();//当前时间
            //写入建模
            //select id from modeinfo where formid = (select id from workflow_bill where tablename='uf_file_monitoring')// 模块id获取
            boolean b = false;
            try {
                String sql = "insert into uf_ygdjl (id,docNo,lcmc,tszt,gdsj,sbyy,dataName,result,detection,formmodeid,modedatacreater,modedatacreatertype,modedatacreatedate,modedatacreatetime) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ";
                logger.info("stepcode:" + stepcode + "      docNo:" + docNo + "    lcmc:" + this.requestId + "    tszt:" + tszt + "    gdsj:" + gdsj + "    sbyy:" + sbyy + "    dataName:" + dataName + "    result:" + result + "    detection:" + detection + "    formmodeid:" + formmodeid + "    DateUtil.today():" + DateUtil.today() + "    DateUtil.formatTime(DateUtil.parse(DateUtil.now())):" + DateUtil.formatTime(DateUtil.parse(DateUtil.now())));
                b = rs.executeUpdate(sql, this.stepcode, docNo, this.requestId, tszt, gdsj, sbyy, dataName, result, detection, formmodeid, 1, 0, DateUtil.today(), DateUtil.formatTime(DateUtil.parse(DateUtil.now())));
                logger.info("b:" + b);
            } catch (Exception e) {
                logger.error("建模表插入失败:", e);
            }
            if (b) {
                // 建模权限重构
                ModeRightInfo ModeRightInfo = new ModeRightInfo();
                ModeRightInfo.setNewRight(true);
                ModeRightInfo.editModeDataShare(Integer.valueOf(formmodeid), 1, stepcode);//新建的时候添加共享
            }
            //发送成功删除临时文件
            delFiles(files);
            long end = System.currentTimeMillis();
            logger.info("档案预归档集成耗费时间:" + (end - start) / 1000.0);
            logger.info("档案预归档集成: end");
        }
        return resultMap;
    }

    //发送成功后删除临时文件
    private void delFiles(List<Map<String, String>> files) {
        for (Map<String, String> file : files) {
            new File(file.get("filepath")).delete();
        }

    }

    /**
     * 获取流程表单pdf
     *
     * @param user       用户
     * @param requestid  流程id必传
     * @param templateId 模板id(传模板id则根据模板生成.不传则默认使用显示模板)
     * @param path       //存储路径(不传则windows默认D:/testpdf;linux默认/usr/testpdf)
     * @return 返回Map包含pdf名称
     */
    public Map<String, Object> getFormPdf(User user, String requestid, String templateId, String path) {
        Map<String, Object> params = new HashMap<>();
        //WorkflowConfigComInfo workflowConfigComInfo = new WorkflowConfigComInfo();
        //String useWk = workflowConfigComInfo.getValue("htmltopdf_usewk");
        params.put("useWk", "1");    //是否使用wkhtmltopdf插件转pdf 1：是  0：否  不传则默认使用Itext插件
        params.put("requestid", requestid);    //必传
        params.put("modeid", templateId);    //模板id(传模板id则根据模板生成.不传则默认使用显示模板)
        params.put("path", path);  //存储路径(不传则windows默认D:/testpdf;linux默认/usr/testpdf)
        params.put("onlyHtml", "2");    //0:转pdf  1:只转成html  2:转html和pdf  (不传则默认=0)
        params.put("keepsign", "1");   //1:保留底部签字意见 0：不保留 (不传则默认=1)
        params.put("pageSize", "100"); //底部签字意见最大显示数量  (默认=100)
        params.put("isTest", "1");    //外部调用必传isTest=1
        params.put("limitauth", "0"); //不校验权限
        HtmlToPdfService htmlToPdfService = (HtmlToPdfService) ServiceUtil.getService(HtmlToPdfServiceImpl.class, user);
        Map<String, Object> pathMap = htmlToPdfService.getFormDatas(params);
        //formPdf = {"pdffilename":"日常报销流程-系统管理员-2022-05-13_20220623162814_pdf.pdf","path":"D:/testpdf","htmlfilename":"日常报销流程-系统管理员-2022-05-13_20220623162814.zip"}
        String pathname = Convert.toStr(pathMap.get("path"));

        if (pathMap.get("htmlfilename") != null) {
            String htmlfilename = Convert.toStr(pathMap.get("htmlfilename"));
            String str = pathname + File.separator + htmlfilename;
            File file = new File(str);
            if (file.exists()) {
                file.delete();
            }
        }
        //修改文件名字
        File file = new File(pathname + File.separator + Convert.toStr(pathMap.get("pdffilename")));
        logger.info("修改前文件名字:" + file.getName());
        //发文：深圳市水务局文件呈批表 (发文)
        //收文：深圳市水务局文件处理表
        //内部呈批：深圳市水务局文件处理表 (内部呈批)
        String newName = "深圳市水务局文件处理表 (内部呈批)";
        if ("发文".equals(typeName)) {
            newName = "深圳市水务局文件呈批表 (发文)";
        } else if ("收文".equals(typeName)) {
            newName = "深圳市水务局文件处理表";
        }
        if (!file.exists()) {
            file.renameTo(new File(file.getParent(), newName));
            pathMap.put("pathNew", pathname + File.separator + newName);
        } else {
            String fileExtension = file.getName().substring(file.getName().lastIndexOf("."));
            int count = 1;
            String tempName = newName;
            while (new File(file.getParent(), tempName + fileExtension).exists()) {
                count++;
                tempName = newName + "(" + count + ")";
            }
            file.renameTo(new File(file.getParent(), tempName + fileExtension));
            pathMap.put("pathNew", pathname + File.separator + tempName + fileExtension);
            logger.info("修改后文件名字:" + pathname + File.separator + tempName + fileExtension);
        }
        File fileNew = new File(Convert.toStr(pathMap.get("pathNew")));

        pathMap.put("disname", fileNew.getName());//母亲身份证.txt
        pathMap.put("operatedatetime", DateUtil.now());
        pathMap.put("filepath", fileNew.getPath());//D:/ddm/yzm.jpg
        pathMap.put("md5", "");//123dwe21321ewq123
        pathMap.put("filetype", typeName+"处理表");//母亲身份证
        pathMap.put("filesize", fileNew.length());//文件大小


        return pathMap;
    }

    private List<Map<String, String>> getFiles(String docids, RecordSet rs, String zw) throws Exception {
        List<Map<String, String>> files = new ArrayList<>();
        File file = new File(path);
        //不存在则创建该目录
        if (!file.exists() && !file.mkdirs()) {
            throw new RuntimeException("创建目录失败: " + file.getAbsolutePath());
        }
        String[] ids = docids.split(",");
        if (ids.length == 0) {
            throw new Exception("附件不能为空");
        }
        logger.info("附件id:" + Arrays.toString(ids));
        for (String id : ids) {
            rs.executeQuery("select i.IMAGEFILEID,i.IMAGEFILENAME,i.FILESIZE,d.docfiletype,d.OPERATEDATE,d.OPERATETIME from imagefile i join docimagefile d on i.imagefileid=d.IMAGEFILEID where docid=?", id);
            if (rs.next()) {
                Map<String, String> filesList = new HashMap<>();
                String fileName = rs.getString("IMAGEFILENAME") == null ? "" : rs.getString("IMAGEFILENAME");
                InputStream is = ImageFileManager.getInputStreamById(rs.getInt("IMAGEFILEID"));
                if (is==null){
                    throw new Exception("附件内容不能为空");
                }
                try (
                        BufferedInputStream bis = new BufferedInputStream(is);
                        //保存文件到这个位置
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path + File.separator + fileName));
                ) {
                    byte[] bytes = new byte[1024 * 8];
                    int length = 0;
                    while ((length = bis.read(bytes)) != -1) {
                        bos.write(bytes, 0, length);
                    }
                } catch (Exception e) {
                    throw e;
                }
                filesList.put("disname", fileName);//母亲身份证.txt
                filesList.put("operatedatetime", rs.getString("OPERATEDATE") + " " + rs.getString("OPERATETIME"));
                filesList.put("filepath", path + File.separator + fileName);//D:/ddm/yzm.jpg
                filesList.put("md5", "");//123dwe21321ewq123
                filesList.put("filetype", zw);//母亲身份证
                filesList.put("filesize", rs.getString("FILESIZE") == null ? "" : rs.getString("FILESIZE"));//文件大小
                /**
                 * {
                 *  "fileformat": "txt(计算机文件格式信息)",
                 *  "disname": "母亲身份证.txt（计算机文件名）",
                 *  "filesize": "12244（文件大小）",
                 *  "filedate": "2023-03-13 16:51:00(计算机文件形成时间)",
                 *  "fileprocedure": "(文档创建程序[软件环境])",
                 *  "manuscriptformat": "docx(原稿格式)",
                 *  "fileorder": "1(件内顺序号)",
                 *  "filepath": "/opt/weaver/ecology/filesystem/hnfileTemp/测试附件1.docx(文件路径)",
                 *  "filetype": "文件处理表（文件类型）",
                 *  "filemanuscript": "正文(稿本)"
                 * }
                 */
                files.add(filesList);
            }
        }
        //文件信息  多个

        return files;
    }

    private Map<String, Object> mapTheResults(Map<String, String> mainTableMap, Map<String, String> oneDataMap, RecordSet rs, List<Map<String, String>> files) {
        /**
         * 	ywid	ywid 	sxlx	sxlx	version	version	稿本	gb	计算机文件大小	jsjwjdx
         * 	紧急程度	jjcd   抄送机关	csjg	原稿格式	yggs	收发文	sfw	发文机关或签发人签名	fwjghqfrqm
         * 	成文日期	cwrq   计算机文件形成时间	jsjwjxcsj	印发日期	yfrq	份号	fh	计算机文件名	jsjwjm
         * 	件内顺序号	jnsxh	签发人	qfr	公开选项	gkxx	收取方式	sqfs	文档创建程序[软件环境]	wdcjcxrjhj
         * 	文件编号[文号]	wjbhwh	主送机关	zsjg	题名	tm	文种	wz	密级	mj	印发机关	yfjg	公文标识	gwbs
         * 	计算机文件格式信息	jsjwjgsxx	发文机关标志	fwjgbz	意见类型	yjlx	处理结果	cljg	处理时间	clsj
         * 	处理类型	cllx	处理部门	clbm	处理者	clz	 filetype	filetype	file	file1	disname	disname
         * 	md5	md5		办理结果	bljg	有效期	yxq
         */
        String jjcd = getValue(mainTableMap, oneDataMap.get("jjcd"));//紧急程度字段
        if (StringUtil.isNotNull(jjcd)) {
            //不等于空进来
            rs.executeQuery("select id,name from  docinstancylevel where id=?", jjcd);
            if (rs.next()) {
                jjcd = rs.getString("name");
            }
        }
        //成文日期
        String cwrq = "";
        String cwrqField="成文日期";
        if (typeName.equals("收文")) {
            cwrqField = "来文日期";
            cwrq = getValue(mainTableMap, oneDataMap.get("cwrq"));
            if (StringUtil.isNotNull(cwrq)) {
                cwrq = LocalDate.parse(cwrq).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            }

        } else {
            //select c.operatedate,c.operatetime from workflow_currentoperator c join workflow_nodebase n on c.nodename='主办处室处长' where n.id='770' and c.requestid='202203'
            rs.executeQuery("select c.operatedate,c.operatetime from workflow_currentoperator c join workflow_nodebase n on c.nodeid=n.id where n.nodename=? and c.requestid=?", this.nodeName, this.requestId);
            if (rs.next()) {
                String operatedate = rs.getString("operatedate");
                if (StringUtil.isNotNull(operatedate)) {
                    cwrq = LocalDate.parse(operatedate).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                }

            }
        }
        //计算机文件形成时间
        String qfr = getValue(mainTableMap, oneDataMap.get("qfr"));
        if (StringUtil.isNotNull(qfr)) {
            //不等于空进来
            User user = new User(Integer.valueOf(qfr));
            qfr = user.getLastname();
        }
        //公开选项
        String gkxx = "";
        String filegkkk = oneDataMap.get("gkxx");
        String filegkkkvalue = getValue(mainTableMap, oneDataMap.get("gkxx"));
        if (StringUtil.isNotNull(filegkkk) && StringUtil.isNotNull(filegkkkvalue)) {
            gkxx = getSeclectName(rs, workflowid, filegkkk, filegkkkvalue);

        }
        //发文机关标志  来文取来文单位；发文默认深圳市水务局
        String fwjgbz = "深圳市水务局";
        if (this.workflowName.contains("来文")) {
            fwjgbz = getValue(mainTableMap, oneDataMap.get("zsjg"));
        }

        String mj = getValue(mainTableMap, oneDataMap.get("mj"));
        if (StringUtil.isNotNull(mj)) {
            rs.executeQuery("select name from docsecretlevel where id=?", mj);
            if (rs.next()) {
                mj = rs.getString("name") == null ? "" : rs.getString("name");
            }
        }
        //文种
        String wztype = getValue(mainTableMap, oneDataMap.get("wz"));
        if (StringUtil.isNotNull(wztype)) {
            rs.executeQuery("select type_name from odoc_odoctype where id=?", wztype);
            if (rs.next()) {
                wztype = rs.getString("type_name") == null ? "" : rs.getString("type_name");
            }
        }

        //获取档案结果建模表id
        if (rs.executeQuery("select max(id) as maxid from uf_ygdjl") && rs.next()) {
            this.stepcode = rs.getInt("maxid") + 1;
        }
        docNo = getValue(mainTableMap, oneDataMap.get("wjbhwh"));
        //basic信息
        Map<String, Object> basic = new HashMap<>();
        basic.put("公文标识", this.requestId);//取requestid值
        basic.put("标题", this.requestName);//流程标题
        basic.put("文号", docNo);//文号
        basic.put("保密期限", "");//保密期限
        basic.put("解密标识", "");//解密标识
        basic.put("附注", "");//解密标识
        basic.put(cwrqField, cwrq);//签发日期8位数（20220102）获取 主办处室处长 节点的操作日期
        basic.put("文种", wztype);//文种
        basic.put("紧急程度", jjcd);//(特提、特急、加急、平急、急件、其他)
        basic.put("主送机关", getValue(mainTableMap, oneDataMap.get("zsjg")));//公文的主要受理机关,应当使用机关全称或规范化简称
        basic.put("抄送机关", getValue(mainTableMap, oneDataMap.get("csjg")));//除主送机关外需要执行或者知晓公文内容的其他机关 这个是文本框
        basic.put("密级", mj);//公开、内部
        basic.put("印发机关", getValue(mainTableMap, oneDataMap.get("yfjg")));//电子公文送印刷机关(取发文)
        basic.put("印发日期", getValue(mainTableMap, oneDataMap.get("yfrq")));//电子公文送印日期(2018-08-12)(取发文)
        basic.put("发文机关标志", fwjgbz);//来文取来文单位；发文默认深圳市水务局
        basic.put("签发人", qfr);//取对外发文
        basic.put("收发文", this.workflowName);//对外发文，外部收文，内部呈批
        basic.put("公开选项", gkxx);//公开方式
        basic.put("份号", files.size());//电子文件份数
        Map<String,String> nodeOperateMap=new HashMap<>();
        if (typeName.equals("收文")&&workflowName.contains("处室办件")){
            nodeOperateMap=getNodeOperate("处室收文员",rs);
        }else if (typeName.equals("收文")){
            nodeOperateMap=getNodeOperate("分办",rs);
        }else {
            nodeOperateMap=getNodeOperate("盖章",rs);
        }
        basic.put("发文机关或签发人签名", "深圳市水务局");//深圳市水务局
        basic.put("收取方式", "在线生成");//在线生成、纸质扫描、归档后补充
        basic.put("oabm", Util.null2String(nodeOperateMap.get("oabm")));//部门名称
        basic.put("oabmid", Util.null2String(nodeOperateMap.get("oabmid")));//部门id


        //select operator,logtype,operatordept,logtype,remark1,operatedate,operatetime from workflow_requestlog  where requestid='117127'
        //流程信息 多个
        List<Map<String, Object>> process = new ArrayList<>();
        rs.executeQuery("select l.operator,l.logtype,d.DEPARTMENTNAME,l.logtype,l.remark,l.operatedate,l.operatetime,n.nodename from workflow_requestlog l left join hrmdepartment d on l.operatordept=d.id left join workflow_nodebase n on n.id=l.nodeid where l.requestid=?", this.requestId);
        while (rs.next()) {
            Map<String, Object> processList = new HashMap<>();
            processList.put("处理类型", rs.getString("nodename")==null?"":rs.getString("nodename"));//履行电子文件形成、处理、管理等业务的具体行为，如拟稿、修改、审稿、审阅、呈批、签发、其他
            processList.put("处理者", new User(rs.getInt("operator")).getLastname());//用户1
            processList.put("处理部门", rs.getString("DEPARTMENTNAME") == null ? "" : rs.getString("DEPARTMENTNAME"));//部门001
            processList.put("意见类型", "");//（可有可无）
            processList.put("处理结果", getText(rs.getString("remark")));//同意
            processList.put("处理时间", rs.getString("operatedate") + " " + rs.getString("operatetime"));//2022-03-17 09:35:15
            process.add(processList);
        }


        //result
        Map<String, Object> result = new HashMap<>();
        result.put("办理结果", getValue(mainTableMap, oneDataMap.get("bljg")));//出证办结
        result.put("有效期", getValue(mainTableMap, oneDataMap.get("yxq")));//有效期  二0二二年三月八日至二0二七年三月七日
        //基本结构
        Map<String, Object> objectMap = new HashMap<>();
        //外部收文：收文  对外发文、内部呈批：发文
        //这个是调用接口的时候 data里面的ywid（nbsp001、fwgd001、swgd001），sxlx（内部审批、收文归档、发文归档）
        String ywid="fwgd001";
        String sxlx="发文归档";
        if (workflowName.contains("内部")){
            ywid="nbsp001";
            sxlx="内部审批";
        }else if(workflowName.contains("收文")){
            ywid="swgd001";
            sxlx="收文归档";
        } else if (workflowName.contains("处室办件")) {
            //ywid：csbj001  sxlx：处室办件
            ywid="csbj001";
            sxlx="处室办件";
        }
        objectMap.put("ywid", ywid);//受理编号	SLBH	唯一  TODO
        objectMap.put("sxlx", sxlx);//事项类型	行政征收事项
        objectMap.put("version", "v1");//事项版本号	1
        objectMap.put("basic", basic);//基本信息
        objectMap.put("process", process);//流程信息
        objectMap.put("files", files);//文件信息
        objectMap.put("result", result);//结果信息
        objectMap.put("stepcode", stepcode);//唯一id
        return objectMap;
    }

    private Map<String,String> getNodeOperate(String nodeName,RecordSet rs) {
        Map<String,String> nodeOperateMap=new HashMap<>();
        String userid="";
        rs.executeQuery("select c.userid from workflow_currentoperator c join workflow_nodebase n on c.nodeid=n.id where n.nodename=? and c.requestid=?", nodeName, this.requestId);
        while (rs.next()){
            userid=rs.getString("userid");
        }
        if (StrUtil.hasEmpty(userid)){
            return nodeOperateMap;
        }
        rs.executeQuery("select d.id,d.departmentname from hrmResource h left join HrmDepartment d on h.departmentid=d.id where h.id=?",userid);
        if (rs.next()){
            nodeOperateMap.put("oabm",rs.getString("departmentname"));
            nodeOperateMap.put("oabmid",rs.getString("id"));

        }
        return nodeOperateMap;

    }

    public String getText(String htmlText){
        if (StrUtil.hasEmpty(htmlText)){
            return "已阅";
        }
        Document doc = Jsoup.parse(htmlText);
        Elements elements = doc.body().select("*");
        for (int i = 0; i < elements.size(); i++) {
            Element el=elements.get(i);
            String text = el.ownText();
            if (!StrUtil.hasEmpty(text)) {
                return text;
            }
        }
        return "";
    }

    private String getTheTypeOfOperation(String logtype) {
        String operationName = "";
        switch (logtype) {
            /**
             * 0：未操作
             * 1：转发
             * 2：已操作
             * 4：归档
             * 5：超时
             * 8：抄送(不需提交)
             * 9：抄送(需提交)
             * a: 意见征询
             * b: 回复
             * h: 转办
             * j: 转办提交
             * 11:传阅
             * 6:自动审批（审批中）
             */
            case "0":
                operationName = "未操作";
                break;
            case "1":
                operationName = "转发";
                break;
            case "2":
                operationName = "已操作";
                break;
            case "4":
                operationName = "归档";
                break;
            case "5":
                operationName = "超时";
                break;
            case "8":
                operationName = "抄送(不需提交)";
                break;
            case "9":
                operationName = "抄送(需提交)";
                break;
            case "a":
                operationName = "意见征询";
                break;
            case "b":
                operationName = "回复";
                break;
            case "h":
                operationName = "转办";
                break;
            case "j":
                operationName = "转办提交";
                break;
            case "11":
                operationName = "传阅";
                break;
            case "6":
                operationName = "自动审批（审批中）";
                break;
            default:
                break;
        }
        return operationName;
    }


    /**
     * 获取下拉框的值，单选框的值
     *
     * @param rs
     * @param workflowId
     * @param fileName
     * @param selectvalue
     * @return
     */
    private String getSeclectName(RecordSet rs, String workflowId, String fileName, String selectvalue) {
        //select selectname from workflow_SelectItem where FIELDID=(select id  from (select id, fieldname, fieldlabel, viewtype, detailtable from workflow_billfield  where billid = (select formid from workflow_base where id='154') and fieldname='gkfs') a   left join htmllabelinfo b on a.fieldlabel = b.indexid and languageid = 7) and selectvalue=1
        String sql = "select selectname from workflow_SelectItem where FIELDID=(select id  from (select id, fieldname, fieldlabel, viewtype, detailtable from workflow_billfield  where billid = (select formid from workflow_base where id= ?) and fieldname= ? ) a   left join htmllabelinfo b on a.fieldlabel = b.indexid and languageid = 7) and selectvalue=?";
        rs.executeQuery(sql, workflowId, fileName, selectvalue);
        if (rs.next()) {
            return rs.getString("selectname");
        }
        return "";
    }

    public static String isNullString(Object value) {
        if (value != null) {
            return String.valueOf(value);
        }
        return "";
    }

    private String getValue(Map<String, String> mainTableMap, String key) {
        if (key != null) {
            return mainTableMap.get(key) == null ? "" : mainTableMap.get(key);
        }
        return "";
    }

    public String updateArchivesInfo(HttpServletRequest request) {
        logger.info("档案预归档更新: start");
        logger.info("接收到参数:" + JSON.toJSONString(request.getParameterMap()));
        RecordSet rs = new RecordSet();
        try {
            String timestamp = request.getHeader("timestamp");
            if (StringUtil.isNull(timestamp)) {
                throw new Exception("timestamp不能为空");
            }
            Date date1 = new Date(Long.valueOf(timestamp));
            Date date2 = DateUtil.parse(DateUtil.now());//当前时间
            long betweenDay = DateUtil.between(date1, date2, DateUnit.MINUTE);
            if (betweenDay > 10) {
                throw new Exception("接口鉴权已过期");
            }
            String sign = request.getHeader("sign");
            if (StringUtil.isNull(sign)) {
                throw new Exception("sign不能为空");
            }
            String key = ArchivesUtil.getParameterValue("ArchivesKey", rs);
            String sha1 = SecureUtil.sha1(timestamp + key);
            if (!sha1.equals(sign)) {
                throw new Exception("接口鉴权未通过");
            }

            /**
             dataName	文件名称     result	档案系统检测结果 deptCode  单位代码
             detection  档案系统检测描述  docNo  办文编号
             */
            String docNo = getValue(request.getParameter("docNo"));
            String dataName = getValue(request.getParameter("dataName"));
            String result = getValue(request.getParameter("result"));
            String deptCode = getValue(request.getParameter("deptCode"));
            String detection = getValue(request.getParameter("detection"));
            StringBuffer dataBuffer = new StringBuffer();
            dataBuffer.append("UPDATE uf_ygdjl set ");
            if (StringUtil.isNotNull(docNo)) {
                dataBuffer.append(" docNo='" + docNo + "',");
            }
            if (StringUtil.isNotNull(dataName)) {
                dataBuffer.append(" dataName='" + dataName + "',");
            }
            //0 不合格 1合格
            if (StringUtil.isNotNull(result)) {
                dataBuffer.append(" result='" + result + "',");
            } else {
                throw new Exception("result不能为空");

            }
            if (StringUtil.isNotNull(deptCode)) {
                dataBuffer.append(" deptCode='" + deptCode + "',");
            }
            if (StringUtil.isNotNull(detection)) {
                dataBuffer.append(" detection='" + detection + "',");
            }
            dataBuffer.deleteCharAt(dataBuffer.length() - 1);//删除最后一位逗号

            String stepcode = getValue(request.getParameter("stepcode"));
            if (StringUtil.isNull(stepcode)) {
                throw new Exception("stepcode不能为空");
            }
            dataBuffer.append("     where id='" + stepcode + "'");
            logger.info("dataBuffer:" + dataBuffer.toString());
            boolean b = rs.executeUpdate(dataBuffer.toString());
            logger.info("b:" + b);
            Map<String, Object> map = new HashMap<>();
            map.put("code", 200);
            map.put("msg", b);
            return JSON.toJSONString(map);
        } catch (Exception e) {
            logger.error("档案预归档更新异常:", e);
            Map<String, Object> map = new HashMap<>();
            map.put("code", 201);
            map.put("msg", e.getMessage());
            return JSON.toJSONString(map);
        } finally {
            logger.info("档案预归档更新: end");
        }
    }

    public String getValue(Object value) {
        if (value != null) {
            return String.valueOf(value);
        }
        return "";
    }


}

