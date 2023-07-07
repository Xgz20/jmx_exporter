package io.prometheus.jmx;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.jmx.common.http.ConfigurationException;
import io.prometheus.jmx.common.http.HTTPServerFactory;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaAgent {

    public static final String CONFIGURATION_REGEX =
            "^(?:((?:[\\w.-]+)|(?:\\[.+])):)?" + // host name, or ipv4, or ipv6 address in brackets
                    "(\\d{1,5}):" +              // port
                    "(.+)";                      // config file

    static HTTPServer server;

    public static void agentmain(String agentArgument, Instrumentation instrumentation) throws Exception {
        premain(agentArgument, instrumentation);
    }

    public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
        // Bind to all interfaces by default (this includes IPv6).
        String host = "0.0.0.0";

        try {
            // 解析命令行参数，主要得到JMX Exporter对外开放的服务地址IP、端口、以及yaml配置文件等
            Config config = parseConfig(agentArgument, host);

            new BuildInfoCollector().register();
            // 创建JMX收集器实例对象，并注册到收集器注册表CollectorRegistry
            new JmxCollector(new File(config.file), JmxCollector.Mode.AGENT).register();
            // 注册Prometheus提供的通用的收集器，例如HotSpot虚拟机的内存信息、线程信息、GC信息等
            DefaultExports.initialize();

            // 创建HTTP Server，这个Server就是提供服务接口供Prometheus调用的
            // 这个Server是Prometheus的子工程client_java提供的
            server =
                    new HTTPServerFactory()
                            .createHTTPServer(
                                    config.socket,
                                    CollectorRegistry.defaultRegistry,
                                    true,
                                    new File(config.file));
        } catch (ConfigurationException e) {
            System.err.println("Configuration Exception : " + e.getMessage());
            System.exit(1);
        }
        catch (IllegalArgumentException e) {
            System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=[host:]<port>:<yaml configuration file> " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Parse the Java Agent configuration. The arguments are typically specified to the JVM as a javaagent as
     * {@code -javaagent:/path/to/agent.jar=<CONFIG>}. This method parses the {@code <CONFIG>} portion.
     * @param args provided agent args
     * @param ifc default bind interface
     * @return configuration to use for our application
     */
    public static Config parseConfig(String args, String ifc) {
        Pattern pattern = Pattern.compile(CONFIGURATION_REGEX);

        Matcher matcher = pattern.matcher(args);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Malformed arguments - " + args);
        }

        String givenHost = matcher.group(1);
        String givenPort = matcher.group(2);
        String givenConfigFile = matcher.group(3);

        int port = Integer.parseInt(givenPort);

        InetSocketAddress socket;
        if (givenHost != null && !givenHost.isEmpty()) {
            socket = new InetSocketAddress(givenHost, port);
        } else {
            socket = new InetSocketAddress(ifc, port);
            givenHost = ifc;
        }

        return new Config(givenHost, port, givenConfigFile, socket);
    }

    static class Config {
        String host;
        int port;
        String file;
        InetSocketAddress socket;

        Config(String host, int port, String file, InetSocketAddress socket) {
            this.host = host;
            this.port = port;
            this.file = file;
            this.socket = socket;
        }
    }
}
