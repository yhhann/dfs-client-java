package com.jingoal.dfsclient;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a URI which can be used to create a DFSClient instance. The URI describes the hosts to
 * be used and options.
 * <p>
 * The format of the URI is:
 *
 * <pre>
 *   dfslb://host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[?options]]
 * </pre>
 *
 * <ul>
 * <li>{@code dfslb://} is a required prefix to identify that this is a string in the standard
 * connection format.</li>
 * <li>{@code host1} is the only required part of the URI. It identifies a server address to connect
 * to.</li>
 * <li>{@code :portX} is optional and defaults to :10000 if not provided.</li>
 * <li>{@code ?options} are connection options. Note that there is a {@code /} required between the
 * last host and the {@code ?} introducing the options. Options are name=value pairs and the pairs
 * are separated by "&amp;".</li>
 * </ul>
 *
 * <p>
 * This supports the following options (case insensitive):
 *
 * <ul>
 * <li>{@code socketTimeoutMS=ms}: How long a send or receive on a socket can take before timing
 * out.</li>
 * <li>{@code socketKeepAlive=false}: whether socket keep alive is enabled.</li>
 * </ul>
 *
 * @see DFSClientOptions for the default values for all options
 */
public class DFSClientURI {
  private static final Logger logger = LoggerFactory.getLogger(DFSClientURI.class);
  private static final String PREFIX = "dfslb://";

  private final DFSClientOptions options;
  private final List<InetSocketAddress> seeds;
  private final String uri;

  /**
   * Creates a DFSClientURI from the given string.
   *
   * @param uri the URI
   */
  public DFSClientURI(final String uri) {
    this(uri, new DFSClientOptions.Builder());
  }

  /**
   * Creates a DFSClientURI from the given URI string and DFSClientOptions.Builder. The builder can
   * be configured with default options, which may be overridden by options specified in the URI
   * string.
   *
   * @param uri the URI
   * @param builder a Builder
   * @see com.jingoal.dfsclient.DFSClientURI#getOptions()
   */
  public DFSClientURI(String uri, DFSClientOptions.Builder builder) {
    this.uri = uri;
    if (!uri.startsWith(PREFIX)) {
      throw new IllegalArgumentException("uri needs to start with " + PREFIX);
    }

    uri = uri.substring(PREFIX.length());

    String serverPart;
    String optionsPart;

    int idx = uri.lastIndexOf("/");
    if (idx < 0) {
      if (uri.contains("?")) {
        throw new IllegalArgumentException("URI contains options without trailing slash");
      }
      serverPart = uri;
      optionsPart = "";
    } else {
      serverPart = uri.substring(0, idx);
      String nsPart = uri.substring(idx + 1);

      idx = nsPart.indexOf("?");
      if (idx >= 0) {
        optionsPart = nsPart.substring(idx + 1);
      } else {
        optionsPart = "";
      }
    }

    // hosts
    List<InetSocketAddress> servers = parseServers(serverPart);
    seeds = Collections.unmodifiableList(servers);

    Map<String, List<String>> optionsMap = parseOptions(optionsPart);
    options = createOptions(optionsMap, builder);
    warnOnUnsupportedOptions(optionsMap);
  }

  private List<InetSocketAddress> parseServers(String serverPart) {
    List<String> serverList = new LinkedList<String>();
    Collections.addAll(serverList, serverPart.split(","));
    List<InetSocketAddress> servers = new ArrayList<InetSocketAddress>(serverList.size());
    for (String svr : serverList) {
      try {
        servers.add(parseServer(svr));
      } catch (UnknownHostException e) {
        logger.error(e.getMessage(), e);
        continue;
      }
    }
    return servers;
  }

  public static final int PORT = 10000;

  /**
   * Returns the default DFS server host: "127.0.0.1"
   *
   * @return IP address of default host.
   */
  public static String defaultHost() {
    return "127.0.0.1";
  }

  /**
   * Returns the default DFS server port: 10000
   *
   * @return the default port
   */
  public static int defaultPort() {
    return PORT;
  }

  private InetSocketAddress parseServer(String svr) throws UnknownHostException {
    if (svr == null) {
      svr = defaultHost();
    }
    svr = svr.trim();
    if (svr.length() == 0) {
      svr = defaultHost();
    }

    int idx = svr.indexOf(":");
    int port = defaultPort();
    if (idx > 0) {
      port = Integer.parseInt(svr.substring(idx + 1));
      svr = svr.substring(0, idx).trim();
    }

    InetSocketAddress _address = new InetSocketAddress(InetAddress.getByName(svr), port);
    return _address;
  }

  static Set<String> allKeys = new HashSet<String>();

  static {
    allKeys.add("sockettimeoutms");
    allKeys.add("socketkeepalive");
  }

  private void warnOnUnsupportedOptions(Map<String, List<String>> optionsMap) {
    for (String key : optionsMap.keySet()) {
      if (!allKeys.contains(key)) {
        logger.warn("Unknown or Unsupported Option '" + key + "'");
      }
    }
  }

  private DFSClientOptions createOptions(Map<String, List<String>> optionsMap,
      DFSClientOptions.Builder builder) {
    for (String key : allKeys) {
      String value = getLastValue(optionsMap, key);
      if (value == null) {
        continue;
      }

      if (key.equals("sockettimeoutms")) {
        builder.socketTimeout(Integer.parseInt(value));
      } else if (key.equals("socketkeepalive")) {
        builder.socketKeepAlive(parseBoolean(value));
      }
    }

    return builder.build();
  }

  private String getLastValue(final Map<String, List<String>> optionsMap, final String key) {
    List<String> valueList = optionsMap.get(key);
    if (valueList == null || valueList.size() == 0) {
      return null;
    }
    return valueList.get(valueList.size() - 1);
  }

  private Map<String, List<String>> parseOptions(String optionsPart) {
    Map<String, List<String>> optionsMap = new HashMap<String, List<String>>();

    for (String _part : optionsPart.split("&")) {
      int idx = _part.indexOf("=");
      if (idx > 0) {
        String key = _part.substring(0, idx).toLowerCase();
        String value = _part.substring(idx + 1);
        List<String> valueList = optionsMap.get(key);
        if (valueList == null) {
          valueList = new ArrayList<String>(1);
        }
        valueList.add(value);
        optionsMap.put(key, valueList);
      }
    }

    return optionsMap;
  }

  private boolean parseBoolean(String _in) {
    String in = _in.trim();
    return in != null && in.length() > 0
        && (in.equals("1") || in.toLowerCase().equals("true") || in.toLowerCase().equals("yes"));
  }

  // ---------------------------------

  /**
   * Gets the list of seeds
   *
   * @return the seed list
   */
  public List<InetSocketAddress> getSeeds() {
    return seeds;
  }

  /**
   * Get the unparsed URI.
   *
   * @return the URI
   */
  public String getURI() {
    return uri;
  }

  /**
   * Gets the options
   *
   * @return the DFSClientOptions based on this URI.
   */
  public DFSClientOptions getOptions() {
    return options;
  }

  @Override
  public String toString() {
    return uri;
  }
}
