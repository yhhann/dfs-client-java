package com.jingoal.dfsclient;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jingoal.dfsclient.cache.DiskCache;
import com.jingoal.dfsclient.cache.PingPangCacheAction;

/**
 * 实现了DSFShardClient (1.0)的接口，是一个Adaptor。
 *
 * @deprecated Replaced by <code>com.jingoal.dfsclient.DFSClientImplV20</code>
 */
@Deprecated
public class DFSClientImpl implements DFSShardClient {
  private static final Logger logger = LoggerFactory.getLogger(DFSClientImpl.class);

  private DFSClientImplV20 instance20;

  /**
   *
   * Load balance for DFS server nodes.If the nodes change, will trigger the ServerListener method
   * onChange, update the consistent hash continuum.
   *
   * DiskCache will support file local cache.
   *
   * <p>
   * Seeds describes the DFS server hosts and options. The format of the seeds is:
   *
   * <pre>
   *   dfslb://host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[?options]]
   * </pre>
   * <ul>
   * <li>{@code dfslb://} is a required prefix to identify that this is a string in the standard
   * connection format.</li>
   * <li>{@code host1} is the only required part of the URI. It identifies a server address to
   * connect to.</li>
   * <li>{@code :portX} is optional and defaults to :10000 if not provided.</li>
   * <li>{@code ?options} are connection options. Note that there is a {@code /} required between
   * the last host and the {@code ?} introducing the options. Options are name=value pairs and the
   * pairs are separated by "&amp;".</li>
   * </ul>
   *
   * <pre>
   * dfslb://192.168.1.1:10000,192.168.1.2:10000
   * dfslb://192.168.1.1,192.168.1.2:10000
   * dfslb://192.168.1.1:10000,192.168.1.2:10000/?k=v
   * dfslb://192.168.1.1,192.168.1.2/?k=v
   * dfslb://192.168.1.1,192.168.1.2:10000/?k=v
   * </pre>
   *
   * @param seeds DFS server seeds.
   * @param clientId The client unique ID.
   * @param diskCache DiskCache.
   */
  public DFSClientImpl(String seeds, String clientId, DiskCache cache) {
    instance20 = new DFSClientImplV20(seeds, clientId, cache);
  }

  /**
   *
   * Load balance for DFS server nodes.If the nodes change, will trigger the ServerListener method
   * onChange, update the consistent hash continuum.
   *
   * DiskCache of primary and secondary will support file local cache.
   *
   * <p>
   * Seeds describes the DFS server hosts and options. The format of the seeds is:
   *
   * <pre>
   *   dfslb://host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[?options]]
   * </pre>
   * <ul>
   * <li>{@code dfslb://} is a required prefix to identify that this is a string in the standard
   * connection format.</li>
   * <li>{@code host1} is the only required part of the URI. It identifies a server address to
   * connect to.</li>
   * <li>{@code :portX} is optional and defaults to :10000 if not provided.</li>
   * <li>{@code ?options} are connection options. Note that there is a {@code /} required between
   * the last host and the {@code ?} introducing the options. Options are name=value pairs and the
   * pairs are separated by "&amp;".</li>
   * </ul>
   *
   * <pre>
   * dfslb://192.168.1.1:10000,192.168.1.2:10000
   * dfslb://192.168.1.1,192.168.1.2:10000
   * dfslb://192.168.1.1:10000,192.168.1.2:10000/?k=v
   * dfslb://192.168.1.1,192.168.1.2/?k=v
   * dfslb://192.168.1.1,192.168.1.2:10000/?k=v
   * </pre>
   *
   * @param seeds DFS server seeds.
   * @param clientId The client unique ID.
   * @param primaryCache DiskCache of primary.
   * @param secondaryCache DiskCache of secondary.
   */
  public DFSClientImpl(String seeds, String clientId, DiskCache primaryCache,
      DiskCache secondaryCache) {
    instance20 = new DFSClientImplV20(seeds, clientId, primaryCache, secondaryCache);
  }

  /**
   * Load balance for DFS server nodes.If the nodes change, will trigger the ServerListener method
   * onChange, update the consistent hash continuum.
   *
   * DiskCache of primary and secondary will support file local cache, and PingPangCacheAction
   * support the chance for handle the event of switch cache.
   *
   * <p>
   * Seeds describes the DFS server hosts and options. The format of the seeds is:
   *
   * <pre>
   *   dfslb://host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[?options]]
   * </pre>
   * <ul>
   * <li>{@code dfslb://} is a required prefix to identify that this is a string in the standard
   * connection format.</li>
   * <li>{@code host1} is the only required part of the URI. It identifies a server address to
   * connect to.</li>
   * <li>{@code :portX} is optional and defaults to :10000 if not provided.</li>
   * <li>{@code ?options} are connection options. Note that there is a {@code /} required between
   * the last host and the {@code ?} introducing the options. Options are name=value pairs and the
   * pairs are separated by "&amp;".</li>
   * </ul>
   *
   * <pre>
   * dfslb://192.168.1.1:10000,192.168.1.2:10000
   * dfslb://192.168.1.1,192.168.1.2:10000
   * dfslb://192.168.1.1:10000,192.168.1.2:10000/?k=v
   * dfslb://192.168.1.1,192.168.1.2/?k=v
   * dfslb://192.168.1.1,192.168.1.2:10000/?k=v
   * </pre>
   *
   * @param seeds DFS server seeds.
   * @param clientId The client unique ID.
   * @param primaryCache DiskCache of primary.
   * @param secondaryCache DiskCache of secondary.
   * @param cacheAction Handle the event of switch cache.
   */
  public DFSClientImpl(String seeds, String clientId, DiskCache primaryCache,
      DiskCache secondaryCache, PingPangCacheAction cacheAction) {
    instance20 = new DFSClientImplV20(seeds, clientId, primaryCache, secondaryCache, cacheAction);
  }

  /**
   * 给定一个已经存在的分布式文件的id,获取其输入流
   *
   * @param fid
   * @return
   * @throws FileNotFoundException
   */
  @Override
  public DFSInputStream getInputStream(String fid, long domain) throws FileNotFoundException {
    try {
      return instance20.getInputStream(fid, domain);
    } catch (InvalidArgumentException e) {
      throw new FileNotFoundException(e.getMessage());
    } catch (IOException e) {
      throw new FileNotFoundException(e.getMessage(), e.getCause());
    }
  }

  /**
   * 通过指定的参数创建一个分布式文件的输出流
   *
   * @param domain 可以传入公司的id等long型数据作为domain,必需
   * @param bizname 模块名,必需
   * @param filename 文件名,可以为null.
   * @return
   * @throws LackofDomainOrBizNameException
   */
  @Override
  public DFSOutputStream getOutputStream(long domain, String bizname, String filename,
      String userid) throws LackofDomainOrBizNameException {
    try {
      return instance20.getOutputStream(domain, bizname, filename, userid);
    } catch (InvalidArgumentException e) {
      throw new LackofDomainOrBizNameException(e.getMessage());
    } catch (IOException e) {
      logger.warn(e.getMessage(), e.getCause());
    }

    return null;
  }

  /**
   * 根据指定的参数创建一个DataSource,参数含义参考getOutputStream()方法.
   *
   * @param domain 可以传入公司的id等long型数据作为domain
   * @param bizname 模块名
   * @param fid 文件id,必需
   * @return
   * @throws LackofDomainOrBizNameException
   */
  @Override
  public DFSDataSource getDataSource(long domain, String bizname, String fid, String userid)
      throws LackofDomainOrBizNameException {
    try {
      return instance20.getDataSource(domain, bizname, fid, userid);
    } catch (FileNotFoundException e) {
      return null;
    } catch (InvalidArgumentException e) {
      throw new LackofDomainOrBizNameException(e.getMessage());
    } catch (IOException e) {
      logger.warn(e.getMessage(), e.getCause());
    }

    return null;
  }

  /**
   * 根据指定的文件id删除分布式文件.
   *
   * @param id
   * @return <code>true</code> if and only if the file is successfully deleted; <code>false</code>
   *         otherwise
   */
  @Override
  public boolean delete(String fid, long domain) {
    try {
      return instance20.delete(fid, domain);
    } catch (InvalidArgumentException e) {
      logger.warn(e.getMessage());
      return false;
    }
  }

  /**
   * 在domain范围内查找md5和size都一样的文件,找到了返回文件复制的fid,找不到返回null.
   *
   * @param domain
   * @param md5
   * @param size
   * @return 找到了返回文件复制的fid,找不到返回null
   */
  @Override
  public String getByMd5(long domain, String md5, long size) {
    try {
      return instance20.getByMd5(domain, md5, size);
    } catch (InvalidArgumentException e) {
      logger.warn(e.getMessage());
    } catch (IOException e) {
      logger.warn(e.getMessage(), e.getCause());
    }

    return null;
  }

  /**
   * 拷贝指定的文件,如果dstdomain和srcdomain相同，进行假复制并且忽略bizname; 否则进行真实文件的拷贝操作。
   *
   * @param dstdomain
   * @param bizname
   * @param srcdomain
   * @param srcFid 被拷贝文件的id
   * @param dstUserid 目的用户的id
   * @return 拷贝的新文件的id
   * @throws FileNotFoundException
   * @throws IOException
   */
  @Override
  public String copy(long dstdomain, String bizname, long srcdomain, String srcFid,
      String dstUserid) throws FileNotFoundException, IOException {
    try {
      return instance20.copy(dstdomain, bizname, srcdomain, srcFid, dstUserid);
    } catch (InvalidArgumentException e) {
      logger.warn(e.getMessage());
    }

    return null;
  }

  /**
   * 给定一个已经存在的分布式文件的id,获取其本地文件
   *
   * @param fid
   * @param domain 要复制的文件属主公司的id
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   */
  @Override
  public File getFile(String fid, long domain) throws FileNotFoundException, IOException {
    try {
      return instance20.getFile(fid, domain);
    } catch (InvalidArgumentException e) {
      throw new FileNotFoundException(e.getMessage());
    }
  }

  /**
   * 通过指定的参数上传文件到分布式文件,返回分布式文件的id,file参数所指定的文件在上传之后被删除,如果还要使用请从getFile得到。
   *
   * @param domain 可以传入公司的id等long型数据作为domain,必需
   * @param bizname 模块名,必需
   * @param filename 文件名,可以为null
   * @param file 本地文件,必需
   * @return 新文件的id
   * @throws LackofDomainOrBizNameException
   * @throws IOException
   */
  @Override
  public String putFile(long domain, String bizname, String filename, File file, String userid)
      throws LackofDomainOrBizNameException, IOException {
    try {
      return instance20.putFile(domain, bizname, filename, file, userid);
    } catch (InvalidArgumentException e) {
      throw new LackofDomainOrBizNameException(e.getMessage());
    }
  }

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
  @Override
  public String putFileNotIntoCache(long domain, String bizname, String filename, File file,
      String userid) throws LackofDomainOrBizNameException, IOException {
    try {
      return instance20.putFileNotIntoCache(domain, bizname, filename, file, userid);
    } catch (InvalidArgumentException e) {
      throw new LackofDomainOrBizNameException(e.getMessage());
    }
  }

  /**
   * 通过指定的参数上传文件到分布式文件,返回分布式文件的id,file参数所指定的文件在上传之后不会被删除，可以继续使用，调用者自行处理该文件。
   *
   * @param domain 可以传入公司的id等long型数据作为domain,必需
   * @param bizname 模块名,必需
   * @param filename 文件名,可以为null
   * @param file 本地文件,必需
   * @return 新文件的id
   * @throws LackofDomainOrBizNameException
   * @throws IOException
   */
  @Override
  public String putFileWithoutMove(long domain, String bizname, String filename, File file,
      String userid) throws LackofDomainOrBizNameException, IOException {
    try {
      return instance20.putFileWithoutMove(domain, bizname, filename, file, userid);
    } catch (InvalidArgumentException e) {
      throw new LackofDomainOrBizNameException(e.getMessage());
    }
  }

  /**
   * 判断指定文件是否存在
   *
   * @param id
   * @param domain
   * @return
   */
  @Override
  public boolean exist(String fid, long domain) {
    try {
      return instance20.exist(fid, domain);
    } catch (InvalidArgumentException e) {
      logger.warn(e.getMessage());
    } catch (IOException e) {
      logger.warn(e.getMessage(), e.getCause());
    }

    return false;
  }

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
  @Override
  public String duplicate(String fid, long domain)
      throws IOException, FileNotFoundException, InvalidArgumentException {
    return instance20.duplicate(fid, domain);
  }

  /**
   * 在domain范围内判断md5和size都一样的文件是否存在.
   *
   * @param domain
   * @param md5
   * @param size
   * @return 找到了返回true, 否则返回false.
   * @throws IOException
   * @throws InvalidArgumentException
   */
  @Override
  public boolean existByMd5(long domain, String md5, long size)
      throws IOException, InvalidArgumentException {
    return instance20.existByMd5(domain, md5, size);
  }
}
