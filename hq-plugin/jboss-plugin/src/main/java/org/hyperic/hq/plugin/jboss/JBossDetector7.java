/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.plugin.jboss;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import javax.naming.Context;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class JBossDetector7 extends DaemonDetector implements AutoServerDetector {

    private static final Log log = LogFactory.getLog(JBossDetector7.class.getName());

    @Override
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List<ServerResource> servers = new ArrayList<ServerResource>();

        long[] pids = getPids("State.Name.sw=java,Args.*.eq=org.jboss.as.standalone");
        for (long pid : pids) {
            HashMap<String, String> args = parseArgs(getProcArgs(pid));
            String version = getVersion(args);
            log.debug("[getServerResources] pid='" + pid + "' version='" + version + "'");
            if (version.startsWith(getTypeInfo().getVersion())) {
                ServerResource server = createServerResource(args.get("jboss.home.dir"));
                setProductConfig(server, getProductConfig(args));
                servers.add(server);
                adjustClassPath(args.get("jboss.home.dir"));

            }
        }
        return servers;
    }

    ConfigResponse getProductConfig(HashMap<String, String> args) {
        ConfigResponse cfg = new ConfigResponse();
        String port = null;
        String address = null;

        String serverConfig = args.get("server-config");
        if (serverConfig == null) {
            serverConfig = "standalone.xml";
        }

        File cfgFile = new File(serverConfig);
        if (!cfgFile.isAbsolute()) {
            String configDir = args.get("jboss.server.config.dir");
            if (configDir == null) {
                String baseDir = args.get("jboss.server.base.dir");
                if (baseDir == null) {
                    baseDir = args.get("jboss.home.dir") + "/standalone";
                }
                configDir = baseDir + "/configuration";
            }
            cfgFile = new File(configDir, serverConfig);
        }
        try {
            log.debug("[getProductConfig] cfgFile=" + cfgFile.getCanonicalPath());
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = (Document) dBuilder.parse(cfgFile);

            XPathFactory factory = XPathFactory.newInstance();
            XPathExpression expr = factory.newXPath().compile("//server/socket-binding-group/socket-binding[@name='jmx-connector-server']");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodeList = (NodeList) result;

            String jmxInterface = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                port = nodeList.item(i).getAttributes().getNamedItem("port").getNodeValue();
                jmxInterface = nodeList.item(i).getAttributes().getNamedItem("interface").getNodeValue();
            }

            if (jmxInterface != null) {
                expr = factory.newXPath().compile("//server/interfaces/interface[@name='" + jmxInterface + "']/inet-address");
                result = expr.evaluate(doc, XPathConstants.NODESET);
                nodeList = (NodeList) result;

                for (int i = 0; i < nodeList.getLength(); i++) {
                    address = nodeList.item(i).getAttributes().getNamedItem("value").getNodeValue();
                }
            }
        } catch (Exception ex) {
            log.debug("Error discovering the jmx.url : " + ex, ex);
        }

        log.debug("[getProductConfig] address='" + address + "' port='" + port + "'");
        if ((address != null) && (port != null)) {
            if (address.startsWith("${")) {
                address = address.substring(2, address.length() - 1);
                if (address.contains(":")) {
                    String[] s = address.split(":");
                    address = args.get(s[0]);
                    if (address == null) {
                        address = s[1];
                    }
                } else {
                    address = args.get(address);
                }
            }

            String jnpUrl = "service:jmx:rmi:///jndi/rmi://" + address + ":" + port + "/jmxrmi";
            cfg.setValue(Context.PROVIDER_URL, jnpUrl);
        }
        return cfg;
    }

    String getVersion(HashMap<String, String> args) {
        String version = "not found";

        String mp = args.get("mp");
        File serverModule = new File(mp, "org/jboss/as/server/main");
        String jars[] = serverModule.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.startsWith("jboss-as-server") && name.endsWith(".jar");
            }
        });

        if ((jars != null) && (jars.length == 1)) {
            try {
                JarFile jarFile = new JarFile(new File(serverModule, jars[0]));
                log.debug("[getVersion] jboss-as-server.jar = '" + jarFile.getName() + "'");
                Attributes attributes = jarFile.getManifest().getMainAttributes();
                jarFile.close();
                version = attributes.getValue("JBossAS-Release-Version");
            } catch (IOException e) {
                log.debug("[getVersion] Error getting JBoss version (" + e + ")", e);
            }
        } else {
            log.debug("[getVersion] 'jboss-as-server.*.jar' not found.");
        }

        return version;
    }

    static HashMap<String, String> parseArgs(String args[]) {
        HashMap<String, String> props = new HashMap<String, String>();
        for (int n = 0; n < args.length; n++) {
            if (args[n].startsWith("-X")) {
            } else if ((args[n].startsWith("-D") || args[n].startsWith("--")) && args[n].contains("=")) {
                String arg[] = args[n].substring(2).split("=");
                props.put(arg[0], arg[1]);
            } else if (args[n].startsWith("-") && args[n].contains("=")) {
                String arg[] = args[n].substring(1).split("=");
                props.put(arg[0], arg[1]);
            } else if (args[n].startsWith("-") && !args[n].contains("=")) {
                props.put(args[n].substring(1), args[n + 1]);
            }
        }
        if (log.isDebugEnabled()) {
            for (String key : props.keySet()) {
                log.debug(key + " = '" + props.get(key) + "'");
            }
        }
        return props;
    }
}
