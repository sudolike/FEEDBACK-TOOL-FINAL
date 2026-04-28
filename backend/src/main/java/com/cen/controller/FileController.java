package com.cen.controller;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cen.mapper.FileMapper;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.common.Result;
import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import com.cen.service.IFileService;
import com.cen.entity.FileD;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 文件上传的列表 前端控制器
 * </p>
 *
 * @author wyt
 * @since 2025-03-09
 */
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    FileMapper fileMapper;
    @Value("${files.upload.path}")
    private  String fileUploadPath;
    @Resource
    private IFileService fileService;
    //新增或修改
    @PostMapping("/save")
    public Result save(@RequestBody FileD fileD) {
        return Result.success(fileService.saveOrUpdate(fileD));
    }
    //删除
    @PostMapping("/delete")
    public Result delete(@RequestBody FileD fileD){ //@RequestBody把前台的json对象转成java的对象
        return Result.success(fileService.removeById(fileD.getId()));
    }
    //批量删除
    @PostMapping("/del/batch")
    public Result Batch(@RequestBody List<Integer> ids){
        return Result.success(fileService.removeBatchByIds(ids));
    }
    //根据id获取
    @GetMapping("/getById")
    public Result findOne(@PathVariable FileD fileD) {
        return Result.success(fileService.getById(fileD.getId()));
    }
    //分页查询
    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize,
                           @RequestParam(defaultValue = "") String name,
                           @RequestParam(defaultValue = "") String type) {
        QueryWrapper<FileD> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(Strings.isNotEmpty(name),"name",name);
        queryWrapper.like(Strings.isNotEmpty(type),"type",type);
        queryWrapper.orderByDesc("id"); //设置id倒序
        return Result.success(fileService.page(new Page<>(pageNum, pageSize),queryWrapper));
    }
    @PostMapping("/upload")
    public String upload(@RequestParam MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String type = FileUtil.extName(originalFilename);
        long size = file.getSize();
        // 定义一个文件唯一的标识码
        String uuid = IdUtil.fastSimpleUUID();
        String fileUUID = uuid + StrUtil.DOT+type;
        File uploadFile = new File(fileUploadPath+fileUUID);
        File parentFile = uploadFile.getParentFile();
        // 判断配置的文件目录是否存在，不存在则创建一个新的文件目录
        if(!uploadFile.getParentFile().exists()){
            uploadFile.getParentFile().mkdirs();
        }
        // 返回相对路径 /file/{uuid}，由客户端按 BASE_URL 拼接，避免 localhost 写死
        String url = "/file/"+fileUUID;
        // 上传文件到磁盘
        file.transferTo(uploadFile);
        // 提取文化的md5
        String md5 = SecureUtil.md5(uploadFile);
        // 充数据库查询是否存在相同的记录
        FileD FileD = getFlieMd5(md5);
        if(FileD!=null){
            url = FileD.getUrl();
            // 由于文件已存在，所以删除刚才上传的重复文件
            uploadFile.delete();
        }
        // 存储数据库
        FileD saveFile = new FileD();
        saveFile.setName(originalFilename);
        saveFile.setType(type);
        saveFile.setSize(size/1024);
        saveFile.setUrl(url);
        saveFile.setMd5(md5);
        fileMapper.insert(saveFile);
        return url;
    }
    /*
     * 文件下载/预览接口
     * 返回正确的 Content-Type 与 inline 头，让客户端可以直接预览图片/PDF/视频
     * */
    @GetMapping("/{fileUUID}")
    public void download(@PathVariable String fileUUID, HttpServletResponse response) throws IOException {
        File uploadFile = new File(fileUploadPath + fileUUID);
        if (!uploadFile.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String ext = FileUtil.extName(fileUUID);
        String mime = guessMime(ext);
        response.setContentType(mime);
        response.setContentLengthLong(uploadFile.length());
        // inline：让客户端内联预览（图片/PDF/视频），下载时仍可右键保存
        response.setHeader(
                "Content-Disposition",
                "inline; filename=\"" + URLEncoder.encode(fileUUID, "UTF-8") + "\""
        );
        // 允许跨域读取（与全局 CORS 配合）
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Cache-Control", "public, max-age=3600");
        ServletOutputStream os = response.getOutputStream();
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(uploadFile.toPath())) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            os.flush();
        }
    }

    private String guessMime(String ext) {
        if (ext == null) return "application/octet-stream";
        switch (ext.toLowerCase()) {
            case "pdf":  return "application/pdf";
            case "png":  return "image/png";
            case "jpg":
            case "jpeg": return "image/jpeg";
            case "gif":  return "image/gif";
            case "webp": return "image/webp";
            case "mp4":  return "video/mp4";
            case "mov":  return "video/quicktime";
            case "ppt":  return "application/vnd.ms-powerpoint";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "doc":  return "application/msword";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":  return "application/vnd.ms-excel";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt":  return "text/plain;charset=UTF-8";
            case "zip":  return "application/zip";
            default:     return "application/octet-stream";
        }
    }
    private FileD getFlieMd5(String md5){
        // 查询文件的md5是否存在
        QueryWrapper<FileD> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("md5",md5);
        List<FileD> fileslist = fileMapper.selectList(queryWrapper);
        return fileslist.size()==0?null:fileslist.get(0);
    }
}

