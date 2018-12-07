package com.jingoal.dfsclient;

import java.io.File;
import java.io.IOException;

import com.jingoal.dfsclient.transfer.FileInfo;

/**
 * 分布式文件系统的客户端接口 v 2.0
 */
public interface DFSShardClientV20 {
  /**
   * 给定一个已经存在的分布式文件的id,获取其输入流.
   *
   * @param fid, 文件ID, required
   * @param domain, 公司ID, required
   * @return
   * @throws IOException
   * @throws FileNotFoundException
   * @throws InvalidArgumentException
   */
  public DFSInputStream getInputStream(String fid, long domain)
      throws IOException, FileNotFoundException, InvalidArgumentException;

  /**
   * 通过指定的参数创建一个分布式文件的输出流.
   *
   * @param domain 公司ID, required
   * @param bizname 模块名, required
   * @param filename 文件名,可以为null
   * @param userid 用户ID, required
   * @return
   * @throws IOException
   * @throws InvalidArgumentException
   */
  public DFSOutputStream getOutputStream(long domain, String bizname, String filename,
      String userid) throws IOException, InvalidArgumentException;

  /**
   * 通过指定的参数创建一个分布式文件的输出流.
   *
   * @param domain 公司ID, required
   * @param bizname 模块名, required
   * @param filename 文件名,可以为null
   * @param userid 用户ID, required
   * @param size File size in byte. When less than 0, will not set timeout.
   * @return
   * @throws IOException
   * @throws InvalidArgumentException
   */
  public DFSOutputStream getOutputStream(long domain, String bizname, String filename,
      String userid, long size) throws IOException, InvalidArgumentException;

  /**
   * 根据指定的参数创建一个 DataSource,参数含义参考 getOutputStream()方法.
   *
   * @param domain 公司ID, required
   * @param bizname 模块名, required
   * @param fid 文件id, required
   * @param userid 用户ID
   * @return
   * @throws IOException
   * @throws FileNotFoundException
   * @throws InvalidArgumentException
   */
  public DFSDataSource getDataSource(long domain, String bizname, String fid, String userid)
      throws IOException, FileNotFoundException, InvalidArgumentException;

  /**
   * 根据指定的文件id删除分布式文件.
   *
   * @param fid 文件id, required
   * @param domain 公司ID, required
   * @return <code>true</code> if and only if the file is successfully deleted; <code>false</code>
   *         otherwise.
   * @throws InvalidArgumentException
   */
  public boolean delete(String fid, long domain) throws InvalidArgumentException;

  /**
   * 添加指定文件的引用.
   *
   * @param fid 文件id, required
   * @param domain 公司ID, required
   * @return 文件复制的fid
   * @throws IOException
   * @throws FileNotFoundException
   * @throws InvalidArgumentException
   */
  public String duplicate(String fid, long domain)
      throws IOException, FileNotFoundException, InvalidArgumentException;

  /**
   * 拷贝指定的文件,如果 dstdomain 和 srcdomain 相同,进行假复制并且忽略bizname; <br>
   * 否则进行真实文件的拷贝操作.
   *
   * @param dstdomain 目的domain, required
   * @param bizname 模块名称, required
   * @param srcdomain 源domain, required
   * @param srcFid 被拷贝文件的id, required
   * @param userid 用户id, required
   * @return 拷贝的新文件的id
   * @throws IOException
   * @throws FileNotFoundException
   * @throws InvalidArgumentException
   */
  public String copy(long dstdomain, String bizname, long srcdomain, String srcFid, String userid)
      throws IOException, FileNotFoundException, InvalidArgumentException;

  /**
   * 在domain范围内查找md5和size都一样的文件,找到了返回文件复制的fid,找不到返回null.
   *
   * @param domain 公司ID, required
   * @param md5 文件的md5值, required
   * @param size 文件大小, required
   * @return 找到了返回文件复制的fid,找不到返回null.
   * @throws IOException
   * @throws InvalidArgumentException
   */
  public String getByMd5(long domain, String md5, long size)
      throws IOException, InvalidArgumentException;

  /**
   * According to the MD5 value to determine whether the existence of the file.
   *
   * @param domain The company id.
   * @param md5 MD5 value.
   * @param size File size.
   * @return
   * @throws IOException
   * @throws InvalidArgumentException
   */
  public boolean existByMd5(long domain, String md5, long size)
      throws IOException, InvalidArgumentException;

  /**
   * 判断指定文件是否存在
   *
   * @param fid 文件ID, required
   * @param domain 公司ID, required
   * @return true if file exist,false if not.
   * @throws IOException
   * @throws InvalidArgumentException
   */
  public boolean exist(String fid, long domain) throws IOException, InvalidArgumentException;

  /**
   * 给定一个已经存在的分布式文件的id,获取其本地文件.
   *
   * @param fid 文件id, required
   * @param domain 公司ID, required
   * @return
   * @throws IOException
   * @throws FileNotFoundException
   * @throws InvalidArgumentException
   */
  public File getFile(String fid, long domain)
      throws IOException, FileNotFoundException, InvalidArgumentException;

  /**
   * 给定一个已经存在的分布式文件的id,获取其文件信息（id,name,size,domain,user,md5,biz）.
   *
   * @param fid 文件id, required
   * @param domain 公司ID, required
   * @return
   * @throws IOException
   * @throws FileNotFoundException
   * @throws InvalidArgumentException
   */
  public FileInfo getFileInfo(String fid, long domain)
      throws IOException, FileNotFoundException, InvalidArgumentException;

  /**
   * 通过指定的参数上传文件到分布式文件,返回分布式文件的id,file参数所指定的文件在上传之后被删除, <br>
   * 如果还要使用请从getFile得到.
   *
   * @param domain 公司ID, required
   * @param bizname 模块名, required
   * @param filename 文件名,可以为null
   * @param file 本地文件, required
   * @param userid 用户ID, required
   * @return 新文件的id
   * @throws IOException
   * @throws InvalidArgumentException
   */
  public String putFile(long domain, String bizname, String filename, File file, String userid)
      throws IOException, InvalidArgumentException;

  /**
   * 通过指定的参数上传文件到分布式文件,与putFile(..)不同之处是本方法不对上传的文件进行缓存.
   *
   * @param domain 公司ID, required
   * @param bizname 模块名, required
   * @param filename 文件名,可以为null
   * @param file 本地文件, required
   * @param userid 用户ID, required
   * @return 新文件的id
   * @throws IOException
   * @throws InvalidArgumentException
   */
  public String putFileNotIntoCache(long domain, String bizname, String filename, File file,
      String userid) throws IOException, InvalidArgumentException;

  /**
   * 通过指定的参数上传文件到分布式文件,返回分布式文件的id,file参数所指定的文件在上传之后不会被删除,<br>
   * 可以继续使用,调用者自行处理该文件.
   *
   * @param domain 公司ID, required
   * @param bizname 模块名, required
   * @param filename 文件名,可以为null
   * @param file 本地文件, required
   * @param userid 用户ID, required
   * @return 新文件的id
   * @throws IOException
   * @throws InvalidArgumentException
   */
  public String putFileWithoutMove(long domain, String bizname, String filename, File file,
      String userid) throws IOException, InvalidArgumentException;
}
