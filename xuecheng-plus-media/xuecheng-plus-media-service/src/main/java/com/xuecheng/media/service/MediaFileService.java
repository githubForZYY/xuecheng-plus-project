package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import org.springframework.web.bind.annotation.RequestBody;

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
}
