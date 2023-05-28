package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;

import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;

import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MinioClient minioClient;

    @Autowired
    MediaFileService currentProxy;

    //普通文件桶
    @Value("${minio.bucket.files}")
    private String bucket_files;
    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }
    //获取文件默认存放路径 年/月/日/
    private String getFolder(){
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd");
        String folder=simpleDateFormat.format(new Date()).replace("-","/")+"/";
        return folder;
    }

    //获取文件的md5
    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            String fileMd5 = DigestUtils.md5Hex(fileInputStream);
            return fileMd5;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //获取文件的md5
    private String getMimeType(String extension){
        if(extension==null)
            extension = "";
        //根据扩展名取出mimeType
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        //通用mimeType，字节流
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if(extensionMatch!=null){
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }
    //将文件上传到minio
    public boolean addFlieToMinio(String bucket,String objectName,String localFilePath,String mimeType){
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs
                    .builder()
                    .bucket(bucket)
                    .object(objectName)
                    .filename(localFilePath)
                    .contentType(mimeType)
                    .build();
            try {
                minioClient.uploadObject(uploadObjectArgs);
            } catch (Exception e) {
                e.printStackTrace();
            }
            log.debug("上传文件到minio成功,bucket:{},objectName:{}",bucket,objectName);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            log.error("上传文件到minio出错,bucket:{},objectName:{},错误原因:{}",bucket,objectName,e.getMessage(),e);
            XueChengPlusException.cast("上传文件到文件系统失败");
        }
        return false;
    }


    //将文件信息插入到文件信息表
    @Transactional
    @Override
    public MediaFiles addFlieInfoToDB(Long companyId,
                                      String fileMd5,
                                      UploadFileParamsDto uploadFileParamsDto,
                                      String bucket,
                                      String objectName){
        //根据md5值查询文件
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        //文件不存在则插入数据
        if (mediaFiles==null) {
            mediaFiles=new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamsDto,mediaFiles);
            mediaFiles.setCompanyId(companyId);
            mediaFiles.setId(fileMd5);
            mediaFiles.setFileId(fileMd5);
            mediaFiles.setFilePath(objectName);
            mediaFiles.setBucket(bucket);
            mediaFiles.setUrl("/"+bucket+"/"+objectName);
            mediaFiles.setCreateDate(LocalDateTime.now());
            mediaFiles.setAuditStatus("002003");
            mediaFiles.setStatus("1");
            int i=mediaFilesMapper.insert(mediaFiles);
            if (i<0){
                log.error("保存文件到数据库表失败:{}",mediaFiles.toString());
                XueChengPlusException.cast("保存文件信息失败");
            }
            log.debug("保存文件到数据库表成功:{}",mediaFiles.toString());
        }
        return mediaFiles;
    }

    //上传文件
    @Override
    public UploadFileResultDto uploadFile(Long companyId,UploadFileParamsDto uploadFileParamsDto,String localFilePath){
        //获取文件
        File file=new File(localFilePath);
        //获取文件目录
        String folder=getFolder();
        //获取存放到minio的文件文件名
        String fileMd5=getFileMd5(file);
        //获取文件名
        String fileName=uploadFileParamsDto.getFilename();
        //获取文件扩展名
        String extension=fileName.substring(fileName.lastIndexOf("."));
        //拼接objectName(带目录)
        String objectName=folder+fileMd5+extension;
        //获取mineType
        String mimeType = getMimeType(extension);
        //上传文件到minio
        boolean b = addFlieToMinio(bucket_files, objectName, localFilePath, mimeType);
        if(!b){
            XueChengPlusException.cast("上传文件失败");
        }
        //插入文件信息到数据库表
        uploadFileParamsDto.setFileSize(file.length());
        //使用代理对象调用addFlieInfoToDB方法，解决事务失效问题
        MediaFiles mediaFiles = currentProxy.addFlieInfoToDB(companyId, fileMd5, uploadFileParamsDto, bucket_files, objectName);
        if (mediaFiles==null) {
            XueChengPlusException.cast("保存文件信息失败");
        }
        UploadFileResultDto uploadFileResultDto=new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles,uploadFileResultDto);
        return uploadFileResultDto;
    }
}
