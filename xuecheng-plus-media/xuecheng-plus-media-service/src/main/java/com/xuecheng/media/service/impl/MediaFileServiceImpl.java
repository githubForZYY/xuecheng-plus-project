package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;

import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;

import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    @Autowired
    MediaProcessMapper mediaProcessMapper;
    //普通文件桶
    @Value("${minio.bucket.files}")
    private String bucket_files;

    //视频文件桶
    @Value("${minio.bucket.videofiles}")
    private String video_files;
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
    @Override
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
                log.error("保存文件信息到数据库表失败:{}",mediaFiles.toString());
                XueChengPlusException.cast("保存文件信息失败");
            }
            //保存待处理文件信息到待处理文件表
            addWaittingMask(mediaFiles);
            log.debug("保存文件信息到数据库表成功:{}",mediaFiles.toString());
        }
        return mediaFiles;
    }

    //添加文件到待处理任务表
    private void addWaittingMask(MediaFiles mediaFiles) {
        //文件名
        String filename = mediaFiles.getFilename();
        //文件后缀名
        String extName = filename.substring(filename.lastIndexOf("."));
        //根据mimeType判断是否加入待处理任务表，(只处理avi类型的文件)
        String mimeType=getMimeType(extName);
        if (mimeType.equals("video/x-msvideo")) {
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(mediaFiles,mediaProcess);
            mediaProcess.setStatus("1");
            mediaProcess.setFailCount(0);
            //保存待处理任务
            mediaProcessMapper.insert(mediaProcess);
        }
    }

    //上传文件
    @Override
    public UploadFileResultDto uploadFile(Long companyId,UploadFileParamsDto uploadFileParamsDto,String localFilePath,String objectName){
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
        //存储到minio中的对象名(带目录)
        if(StringUtils.isEmpty(objectName)){
            objectName =  folder + fileMd5 + extension;
        }
//        String objectName = defaultFolderPath + fileMd5 + extension;
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

    //查询文件是否已存储
    @Override
    public RestResponse<Boolean> checkFile(String md5) {
        //查询数据库表是否存在该md5值的文件信息
        MediaFiles mediaFiles=mediaFilesMapper.selectById(md5);
        if (mediaFiles != null) {
            //获取文件存放路径信息
            String bucket=mediaFiles.getBucket();
            String object=mediaFiles.getFilePath();
            GetObjectArgs objectArgs = GetObjectArgs.builder().bucket(bucket).object(object).build();
            try {
                InputStream stream=minioClient.getObject(objectArgs);
                if (stream != null) {
                    //文件已存在
                    RestResponse.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } 
        }
        //文件不存在
        return RestResponse.success(false);
    }

    //查询块是否已传输
    @Override
    public RestResponse<Boolean> checkChunk(String md5,int chunkIndex) {
        String chunkPath=getChunkPath(md5)+chunkIndex;
        GetObjectArgs objectArgs = GetObjectArgs.builder().bucket(video_files).object(chunkPath).build();
        try {
            InputStream stream=minioClient.getObject(objectArgs);
            //块已传输
            if (stream != null) {
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResponse.success(false);
    }
    
    //根据块md5值和块索引获取块在minio的存放路径
    private String getChunkPath(String md5) {
        return md5.substring(0,1)+"/"+md5.substring(1,2)+"/"+md5+"/"+"chunk"+"/";
    }

    //上传块
    @Override
    public RestResponse<Boolean> uploadChunk(String md5,int chunkIndex,String localFilePath){
        //获取块在minio的存放路径
        String chunkPath=getChunkPath(md5)+chunkIndex;
        try {
            UploadObjectArgs objectArgs= UploadObjectArgs
                    .builder()
                    .bucket(video_files)
                    .object(chunkPath)
                    .filename(localFilePath)
                    .build();
             minioClient.uploadObject(objectArgs);
             //打印日志
            log.debug("上传块:{}到桶:{}成功",chunkPath, video_files);
             //上传成功
             return RestResponse.success(true);
        } catch (Exception e) {
            //上传失败
            log.error("上传块:{}到桶:{}失败",chunkPath, video_files);
            e.printStackTrace();
        }
        return RestResponse.validfail(false,"上传块失败");
    }

    //合并文件
    @Override
    public RestResponse<Boolean> mergeChunk(long companyId,String fileMd5,UploadFileParamsDto dto,int chunkTotal) {
        //获取分块文件夹路径
        String chunkFloderPath=getChunkPath(fileMd5);
        //组成将分块文件路径组成 List<ComposeSource>
        List<ComposeSource> sourceObjectList = Stream.iterate(0, i -> ++i)
                .limit(chunkTotal)
                .map(i -> ComposeSource.builder()
                        .bucket(video_files)
                        .object(chunkFloderPath.concat(Integer.toString(i)))
                        .build())
                .collect(Collectors.toList());
        //获取文件名
        String fileName = dto.getFilename();
        //获取文件扩展名
        String extName=fileName.substring(fileName.lastIndexOf("."));
        String mergeFilePath=getMergeFilePathByMd5(fileMd5,extName);
        //合并块
        try {
            ObjectWriteResponse objectWriteResponse = minioClient.composeObject(
                    ComposeObjectArgs
                            .builder()
                            .bucket(video_files)
                            .object(mergeFilePath)
                            .sources(sourceObjectList)
                            .build()
            );
            log.debug("合并块列表:{}到桶:{}成功",sourceObjectList,video_files);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并块列表:{}到桶:{}失败",sourceObjectList,video_files);
        }
        //下载合并后文件
        File minioFile=downloadFileByMinio(video_files,mergeFilePath);
        if (minioFile == null) {
            log.error("下载文件:{}从桶:{}失败",fileName,video_files);
            return RestResponse.validfail(false,"下载合并后文件失败");
        }//下载成功，验证md5值
        try {
            FileInputStream fileInputStream=new FileInputStream(minioFile);
            String mergeFileMd5="";
            try {
                mergeFileMd5=DigestUtils.md5Hex(fileInputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!fileMd5.equals(mergeFileMd5)) {
                return RestResponse.validfail(false,"上传文件失败");
            }
            dto.setFileSize(minioFile.length());
        } catch (FileNotFoundException e) {
            log.error("校验合并后文件:{}md5值{}出错",fileName,fileMd5);
            e.printStackTrace();
            return RestResponse.validfail(false,"上传文件失败");
        }finally {
            if (minioFile != null) {
                minioFile.delete();
            }
        }
        //将合并后的文件信息入库
        currentProxy.addFlieInfoToDB(companyId,fileMd5,dto,video_files,mergeFilePath);
        //清理块文件
        clearChunkFiles(chunkFloderPath,chunkTotal);
        return RestResponse.success(true,"文件上传成功");
    }
    //获取合并后的文件路径
    private String getMergeFilePathByMd5(String fileMd5,String extName){
        return fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+extName;
    }

    //从minio下载文件
    @Override
    public File downloadFileByMinio(String bucket,String filePath){
        GetObjectArgs build = GetObjectArgs.builder().bucket(bucket).object(filePath).build();
        OutputStream outputStream=null;
        try {
            InputStream stream = minioClient.getObject(build);
            File fileTemp=File.createTempFile("minio",".temp");
            outputStream=new FileOutputStream(fileTemp);
            IOUtils.copy(stream,outputStream);
            return fileTemp;
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    //清除块文件
    private void clearChunkFiles(String chunkFileFolderPath,int chunkTotal) {

        try {
            List<DeleteObject> deleteObjects = Stream.iterate(0, i -> ++i)
                    .limit(chunkTotal)
                    .map(i -> new DeleteObject(chunkFileFolderPath.concat(Integer.toString(i))))
                    .collect(Collectors.toList());

            RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket("video").objects(deleteObjects).build();
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
            results.forEach(r -> {
                DeleteError deleteError = null;
                try {
                    deleteError = r.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("清楚分块文件失败,objectname:{}", deleteError.objectName(), e);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清楚分块文件失败,chunkFileFolderPath:{}", chunkFileFolderPath, e);
        }
    }

    @Override
    public MediaFiles getFileById(String mediaId) {
        return mediaFilesMapper.selectById(mediaId);
    }
}
