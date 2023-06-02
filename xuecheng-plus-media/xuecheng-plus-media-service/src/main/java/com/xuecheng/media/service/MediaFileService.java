package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.File;
import java.util.List;

/**
 * @description 媒资文件管理业务类
 * @author Mr.M
 * @date 2022/9/10 8:55
 * @version 1.0
 */
public interface MediaFileService {

 /**
  * @description 媒资文件查询方法
  * @param pageParams 分页参数
  * @param queryMediaParamsDto 查询条件
  * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
  * @author Mr.M
  * @date 2022/9/10 8:57
 */
PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

 /**
  * 上传图片
  * @param companyId 机构id
  * @param uploadFileParamsDto 上传文件参数
  * @param localFilePath 本地文件路径
  * @return
  */
 UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath);

 /**
  * 将文件信息插入到数据库表
  * @param companyId
  * @param fileMd5
  * @param uploadFileParamsDto
  * @param bucket
  * @param objectName
  * @return
  */
 MediaFiles addFlieInfoToDB(Long companyId,
                                   String fileMd5,
                                   UploadFileParamsDto uploadFileParamsDto,
                                   String bucket,
                                   String objectName);

 /**
  * 查询该文件是否已存在
  * @param md5 文件的md5值
  * @return RestResponse<Boolean> 响应类型，Boolean值为true表示文件已存在，反之不存在
  */
 RestResponse<Boolean> checkFile(String md5);

 /**
  *
  * @param md5 块的md5值
  * @param chunkIndex 块的索引
  * @return RestResponse<Boolean> 响应类型，Boolean值为true表示此块已存在，反之不存在
  */
 RestResponse<Boolean> checkChunk(String md5,int chunkIndex);

 /**
  *
  * @param md5 块md5值
  * @param chunkIndex 块索引
  * @param localFilePath 本地文件路径
  * @return
  */
 RestResponse<Boolean> uploadChunk(String md5,int chunkIndex,String localFilePath);

 /**
  * 合并文件
  * @param companyId 机构id
  * @param fileMd5 文件md5值
  * @param dto 上传文件参数对象
  * @param chunkTotal 块总数
  * @return
  */
 RestResponse<Boolean> mergeChunk(long companyId,String fileMd5,UploadFileParamsDto dto,int chunkTotal);

 /**
  * 从minio下载文件
  * @param bucket 桶
  * @param filePath 文件路径
  * @return
  */
 File downloadFileByMinio(String bucket, String filePath);

 /**
  * 上传文件到minio
  * @param bucket
  * @param objectName
  * @param localFilePath
  * @param mimeType
  * @return
  */
 boolean addFlieToMinio(String bucket,String objectName,String localFilePath,String mimeType);

 MediaFiles getFileById(String mediaId);
}


