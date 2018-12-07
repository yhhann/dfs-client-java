package com.jingoal.dfsclient;

import java.io.File;
import java.io.IOException;

/**
 * 分布式文件系统的客户端接口 v 1.0
 *
 * @deprecated Replaced by <code>com.jingoal.dfsclient.DFSShardClientV20</code>
 */
@Deprecated
public interface DFSShardClient {
  /**
   * 给定一个已经存在的分布式文件的id,获取其输入流
   *
   * @param fid
   * @return
   * @throws FileNotFoundException
   */
  public DFSInputStream getInputStream(String fid, long domain) throws FileNotFoundException;

  /**
   * 通过指定的参数创建一个分布式文件的输出流
   *
   * @param domain 可以传入公司的id等long型数据作为domain,必需
   * @param bizname 模块名,必需
   * @param filename 文件名,可以为null.
   * @return
   * @throws LackofDomainOrBizNameException
   */
  public DFSOutputStream getOutputStream(long domain, String bizname, String filename,
      String userid) throws LackofDomainOrBizNameException;

  /**
   * 根据指定的参数创建一个DataSource,参数含义参考getOutputStream()方法.
   *
   * @param domain 可以传入公司的id等long型数据作为domain
   * @param bizname 模块名
   * @param fid 文件id,必需
   * @return
   * @throws LackofDomainOrBizNameException
   */
  public DFSDataSource getDataSource(long domain, String bizname, String fid, String userid)
      throws LackofDomainOrBizNameException;

  /**
   * 根据指定的文件id删除分布式文件.
   *
   * @param id
   * @return <code>true</code> if and only if the file is successfully deleted; <code>false</code>
   *         otherwise
   */
  public boolean delete(String id, long domain);

  /**
   * 在domain范围内查找md5和size都一样的文件
   *
   * @param domain
   * @param md5
   * @param size
   * @return 找到了返回文件复制的fid,找不到返回null
   */
  public String getByMd5(long domain, String md5, long size);

  /**
   * 拷贝指定的文件,如果dstdomain和srcdomain相同，进行假复制并且忽略bizname; 否则进行真实文件的拷贝操作。
   *
   * @param dstdomain
   * @param bizname
   * @param srcdomain
   * @param oid 被拷贝文件的id
   * @param dstUserid 目的用户的id
   * @return 拷贝的新文件的id
   * @throws FileNotFoundException
   * @throws IOException
   */
  public String copy(long dstdomain, String bizname, long srcdomain, String oid, String dstUserid)
      throws FileNotFoundException, IOException;

  /**
   * 给定一个已经存在的分布式文件的id,获取其本地文件
   *
   * @param fid
   * @param domain 要复制的文件属主公司的id
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   */
  public File getFile(String fid, long domain) throws FileNotFoundException, IOException;

  /**
   * 通过指定的参数上传文件到分布式文件,返回分布式文件的id,file参数所指定的文件在 上传之后被删除,如果还要使用请从getFile得到。
   *
   * @param domain 可以传入公司的id等long型数据作为domain,必需
   * @param bizname 模块名,必需
   * @param filename 文件名,可以为null
   * @param file 本地文件,必需
   * @return 新文件的id
   * @throws LackofDomainOrBizNameException
   * @throws IOException
   */
  public String putFile(long domain, String bizname, String filename, File file, String userid)
      throws LackofDomainOrBizNameException, IOException;

  /**
   * 通过指定的参数上传文件到分布式文件,与putFile(..)不同之处是本方法不对上传的文件进行缓存。
   *
   * @param domain
   * @param bizname
   * @param filename
   * @param file
   * @param userid
   * @return
   * @throws LackofDomainOrBizNameException
   * @throws IOException
   */
  public String putFileNotIntoCache(long domain, String bizname, String filename, File file,
      String userid) throws LackofDomainOrBizNameException, IOException;

  /**
   * 通过指定的参数上传文件到分布式文件,返回分布式文件的id,file参数所指定的文件 在上传之后不会被删除，可以继续使用，调用者自行处理该文件。
   *
   * @param domain 可以传入公司的id等long型数据作为domain,必需
   * @param bizname 模块名,必需
   * @param filename 文件名,可以为null
   * @param file 本地文件,必需
   * @return 新文件的id
   * @throws LackofDomainOrBizNameException
   * @throws IOException
   */
  String putFileWithoutMove(long domain, String bizname, String filename, File file, String userid)
      throws LackofDomainOrBizNameException, IOException;

  /**
   * 判断指定文件是否存在
   *
   * @param id
   * @param domain
   * @return
   */
  public boolean exist(String id, long domain);

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
   * 在domain范围内判断md5和size都一样的文件是否存在.
   *
   * @param domain
   * @param md5
   * @param size
   * @return 找到了返回true, 否则返回false.l
   * @throws IOException
   * @throws InvalidArgumentException
   */
  public boolean existByMd5(long domain, String md5, long size)
      throws IOException, InvalidArgumentException;
}
