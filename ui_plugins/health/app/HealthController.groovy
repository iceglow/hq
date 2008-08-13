import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl as DMM
import org.hyperic.hq.bizapp.server.session.ProductBossEJBImpl as PB
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl as SCM
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl
import org.hyperic.hq.appdef.server.session.AgentSortField
import org.hyperic.hq.appdef.Agent
import org.hyperic.util.PrintfFormat
import org.hyperic.util.units.UnitsFormat
import org.hyperic.util.units.UnitsConstants
import org.hyperic.util.units.UnitNumber
import org.hyperic.hq.hqu.rendit.html.HtmlUtil
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.hqu.rendit.util.HQUtil
import org.hyperic.hq.common.DiagnosticThread
import org.hyperic.hq.common.Humidor
import org.hyperic.util.jdbc.DBUtil
import org.hyperic.hibernate.PageInfo

import java.text.DateFormat;
import java.sql.Types
import javax.naming.InitialContext

import groovy.sql.Sql

import net.sf.ehcache.CacheManager


class HealthController 
	extends BaseController
{
    private final DateFormat df = 
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    private final PrintfFormat agentFmt =
        new PrintfFormat("%-25s %-15s %-5s %-9s %-17s %-13s %-16s %-10s %s")

    HealthController() {
        onlyAllowSuperUsers()
        setJSONMethods(['getSystemStats', 'getDiag', 'cacheData', 
                        'agentData', 'runQuery'])
    }
    
    private getAgentSchema() {
        def res = [
            getData: {pageInfo, params ->
                getAgentData(pageInfo)
            },
            defaultSort: AgentSortField.CTIME,
            defaultSortOrder: 1,
            rowId: {it.id},
            columns: [
                [field: [getValue: {localeBundle.fqdn},
                         description:'fqdn', sortable:false], 
                 width: '20%',
                 label: {it.platformHtml}],
                [field: AgentSortField.ADDR,
                 width: '15%',
                 label: {it.serverHtml}],
                [field: AgentSortField.PORT,
                 width: '5%',
                 label: {it.agent.port}],
                [field: AgentSortField.VERSION,
                 width: '10%',
                 label: {it.agent.version}],
                [field: AgentSortField.CTIME,
                 width: '18%',
                 label: {it.creationTime}],
                [field: [getValue: {localeBundle.numPlatforms},
                         description:'numPlatforms', sortable:false], 
                 width: '8%',
                 label: {it.agent.platforms.size()}],
                [field: [getValue: {localeBundle.numMetrics},
                         description:'numMetrics', sortable:false], 
                 width: '10%',
                 label: {it.numMetrics}],
                [field: [getValue: {localeBundle.timeOffset},
                         description:'timeOffset', sortable:false], 
                 width: '19%',
                 label: {it.offsetHtml}],
            ],
        ]   

        if (HQUtil.isEnterpriseEdition()) {
            res.columns << [
                field: [getValue: {localeBundle.licenseCount},
                        description:'licenseCount', sortable:false],
                width: '10%',
                label: {it.licenseCount}]
        }
        
        res
    }
    
    private getLicenseCount(Agent a) {
        if (!HQUtil.isEnterpriseEdition())
            return ""
            
        def lman = com.hyperic.hq.license.LicenseManager.instance()
        "${lman.getCountPerAgent(a)}"
    }
    
    private getAgentData(pageInfo) {
        def res = []
        def agents     = agentHelper.find(withPaging: pageInfo)
        def offsetData = DMM.one.findAgentOffsetTuples()
        def metricData = DMM.one.findNumMetricsPerAgent()
        for (a in agents) {
            def found = false
            def numMetrics = 0
            
            if (metricData[a])
                numMetrics = metricData[a]
            for (d in offsetData) {
                if (d[0] == a) {
                    def metricVal = d[3].lastDataPoint?.value
                    if (metricVal == null)
                        metricVal = '?'
                    
                    res << [agent:a, 
                            platform:d[1].fqdn,
                            platformHtml:linkTo(d[1].fqdn, [resource:d[1].resource]),
                            server:a.address,
                            serverHtml:linkTo(a.address, [resource:d[2].resource]),
                            offset:metricVal,
                            offsetHtml:linkTo(metricVal, [resource:d[3]]), 
                            numMetrics:numMetrics,
                            creationTime:df.format(a.creationTime),
                            licenseCount:getLicenseCount(a)]
                    found = true
                    break
                }
            }
            if (!found) {
                res << [agent:a, numMetrics:numMetrics,
                        platform:'Unknown', platformHtml:'Unknown',
                        server:a.address, serverHtml:a.address,
                        offset:'?', offsetHtml:'?', 
                        creationTime:df.format(a.creationTime),
                        licenseCount:getLicenseCount(a)]
            }
        }
        res
    }
    
    def agentData(params) {
        DojoUtil.processTableRequest(agentSchema, params)
    }
    
    private getCacheSchema() {
        def regionCol = new CacheColumn('region', 'Region', true)
        def sizeCol   = new CacheColumn('size',   'Size',   true)
        def hitsCol   = new CacheColumn('hits',   'Hits',   true)
        def missCol   = new CacheColumn('misses', 'Misses', true)
        
        def globalId = 0
        [
            getData: {pageInfo, params ->
                getCacheData(pageInfo)
            },
            defaultSort: regionCol,
            defaultSortOrder: 1,  // descending
            rowId: {globalId++},
            styleClass: {(it.misses <= it.size) ? null : "red"},
            columns: [
                [field:  regionCol,
                 width:  '50%',
                 label:  {it.region}],
                [field:  sizeCol,
                 width:  '10%',
                 label:  {"${it.size}"}],
                [field:  hitsCol,
                 width:  '10%',
                 label:  {"${it.hits}"}],
                [field:  missCol,
                 width:  '10%',
                 label:  {"${it.misses}"}],
            ],
        ]
    }
    
	private getCacheData(pageInfo) {
	    def cm = CacheManager.instance
	    def res = []
	               
	    for (name in cm.cacheNames) {
	        def cache = cm.getCache(name)
	        res << [region: name,
	                size:   cache.size,
	                hits:   cache.hitCount,
	                misses: cache.missCountNotFound]
	    }
	    
	    def d = pageInfo.sort.description
	    res = res.sort {a, b ->
	        return a."${d}" <=> b."${d}"
	    }
	    if (!pageInfo.ascending) 
	        res = res.reverse()
	    
	    // XXX:  This is still incorrect
	    def startIdx = pageInfo.startRow
	    def endIdx   = startIdx + pageInfo.pageSize
	    if (endIdx >= res.size)
	        endIdx = -1
	    return res[startIdx..endIdx]
    }

	private getDiagnostics() {
	    DiagnosticThread.diagnosticObjects.sort {a, b -> a.name <=> b.name }
	}
	
	def index(params) {
    	render(locals:[ 
    	    diags:             diagnostics,
    	    cacheSchema:       cacheSchema,
    	    agentSchema:       agentSchema,
    	    metricsPerMinute:  metricsPerMinute,
    	    numPlatforms:      resourceHelper.find(count:'platforms'),
    	    numServers:        resourceHelper.find(count:'servers'),
    	    numServices:       resourceHelper.find(count:'services'),
    	    numAgents:         agentHelper.find(count:'agents'),
    	    databaseQueries:   databaseQueries,
    	    jvmSupportsTraces: getJVMSupportsTraces() ])
    }
    
	private getMetricsPerMinute() {
	    def vals  = DMM.one.findMetricCountSummaries()
	    def total = 0.0
	    
	    for (v in vals) {
	        total = total + (float)v.total / (float)v.interval
	    }
	    (int)total
	}
	
    def getDiag(params) {
        def diagName = params.getOne('diag')
        for (d in diagnostics) {
            if (d.shortName == diagName) {
                return [diagData: '<pre>' + d.status + '</pre>']
            }
        }
    }
    
    def cacheData(params) {
        DojoUtil.processTableRequest(cacheSchema, params)
    }

    private formatBytes(b) {
        if (b == -1)
            return 'unknown'
            
        UnitsFormat.format(new UnitNumber(b, UnitsConstants.UNIT_BYTES,
                                          UnitsConstants.SCALE_NONE),
                           locale, null).toString()
    }

    private formatPercent(n, total) {
        if (total == 0)
           return 0
        return (int)(n * 100 / total)
    }

    def getSystemStats(params) {
        def s = Humidor.instance.sigar
        def loadAvgFmt = new PrintfFormat('%.2f')
        def dateFormat = DateFormat.getDateTimeInstance()
        
        def cpu      = s.cpuPerc
        def sysMem   = s.mem
        def sysSwap  = s.swap
        def pid      = s.pid
        def procFds  = 'unknown'
        def procMem  = s.getProcMem(pid)
        def procCpu  = s.getProcCpu(pid)
        def procTime = s.getProcTime(pid)
        def NA       = 'N/A' //XXX localeBundle?
        def loadAvg1 = NA
        def loadAvg5 = NA
        def loadAvg15 = NA
        def runtime  = Runtime.runtime
            
        try {
            procFds = s.getProcFd(pid).total
        } catch(Exception e) {
        }

        try {
            def loadAvg = s.loadAverage
            loadAvg1  = loadAvgFmt.sprintf(loadAvg[0])
            loadAvg5  = loadAvgFmt.sprintf(loadAvg[1])
            loadAvg15 = loadAvgFmt.sprintf(loadAvg[2])
        } catch(Exception e) {
            //SigarNotImplementedException on Windows
        }

        //e.g. Linux
        def free;
        def used;
        if ((sysMem.free != sysMem.actualFree ||
            (sysMem.used != sysMem.actualUsed))) {
            free = sysMem.actualFree
            used = sysMem.actualUsed
        } else {
            free = sysMem.free
            used = sysMem.used
        }

        return [sysUserCpu:    (int)(cpu.user * 100),
                sysSysCpu:     (int)(cpu.sys * 100),
                sysNiceCpu:    (int)(cpu.nice * 100),
                sysIdleCpu:    (int)(cpu.idle * 100),
                sysWaitCpu:    (int)(cpu.wait * 100),
                sysPercCpu:    (int)(100 - cpu.idle * 100),
                loadAvg1:      loadAvg1,
                loadAvg5:      loadAvg5,
                loadAvg15:     loadAvg15,
                totalMem:      formatBytes(sysMem.total),
                usedMem:       formatBytes(used),
                freeMem:       formatBytes(free),
                percMem:       formatPercent(used, sysMem.total),
                totalSwap:     formatBytes(sysSwap.total),
                usedSwap:      formatBytes(sysSwap.used),
                freeSwap:      formatBytes(sysSwap.free),
                percSwap:      formatPercent(sysSwap.used, sysSwap.total),
                pid:           pid,
                procStartTime: dateFormat.format(procTime.startTime),
                procOpenFds:   procFds,
                procMemSize:   formatBytes(procMem.size),
                procMemRes:    formatBytes(procMem.resident),
                procMemShare:  formatBytes(procMem.share),
                procCpu:       formatPercent(procCpu.percent, runtime.availableProcessors()),
                jvmTotalMem:   formatBytes(runtime.totalMemory()),
                jvmFreeMem:    formatBytes(runtime.freeMemory()),
                jvmMaxMem:     formatBytes(runtime.maxMemory()),
                jvmPercMem:    formatPercent(runtime.maxMemory() - runtime.freeMemory(), runtime.maxMemory()),
        ]
    }
    
    def printReport(params) {
        def s = Humidor.instance.sigar
        def dateFormat  = DateFormat.dateTimeInstance
        def cmdLine     = s.getProcArgs('$$')
        def procEnv     = s.getProcEnv('$$')
        def agentPager  = PageInfo.getAll(AgentSortField.ADDR, true) 
        
        def locals = [
            numCpu:           Runtime.runtime.availableProcessors(),
            fqdn:             s.getFQDN(),
            guid:             SCM.one.getGUID(),
            reportTime:       dateFormat.format(System.currentTimeMillis()),
            userName:         user.fullName,
            numAgents:        AgentManagerEJBImpl.one.agentCount,
            metricsPerMinute: metricsPerMinute,
            diagnostics:      diagnostics,
            hqVersion:        PB.one.version,
            buildNumber:      PB.one.buildNumber,
            jvmProps:         System.properties,
            schemaVersion:    SCM.one.config.getProperty('CAM_SCHEMA_VERSION'),
            cmdLine:          cmdLine,
            procEnv:          procEnv,
            cpuInfos:         s.cpuInfoList,
            jvmSupportsTraces: getJVMSupportsTraces(),
            agentData:        getAgentData(agentPager),
            agentFmt:         agentFmt,
            AgentSortField:   AgentSortField,
            licenseInfo:      [:],
        ] + getSystemStats([:])
        
        if (HQUtil.isEnterpriseEdition()) {
            locals.licenseInfo = com.hyperic.hq.license.LicenseManager.licenseInfo
        }
        
    	render(locals: locals)
    }
    
    private withConnection(Closure c) {
        def ctx  = new InitialContext()
        def ds   = ctx.lookup("java:/HypericDS")
        def conn
        
        try {
            conn = ds.connection
            return c.call(conn)
        } finally {
            if (conn != null) conn.close()
        }
    }
    
    private getDatabaseQueries() {
        def queries = [ 
          pgLocks: [ 
             name: localeBundle['queryPostgresLocks'], 
             viewable: {conn -> DBUtil.isPostgreSQL(conn) },          
             query: "select l.mode, transaction, l.granted, " + 
                    "now() - query_start as time, current_query " + 
                    "from pg_locks l, pg_stat_activity a " + 
                    "where l.pid=a.procpid " + 
                    " and now() - query_start > '00:00:01'"],
          pgStatActivity: [ 
             name: localeBundle['queryPostgresActivity'], 
             viewable: {conn -> DBUtil.isPostgreSQL(conn) },          
             query: "select * from pg_stat_activity " + 
                    "where current_query != '<IDLE>' order by query_start desc"],
          aiqPlatform: [ 
             name: localeBundle['queryAIQPlatform'], 
             query: "select * from EAM_AIQ_PLATFORM"], 
          aiqServer: [ 
             name: localeBundle['queryAIQServer'], 
             query: "select * from EAM_AIQ_SERVER"],
          aiqIP: [ 
             name: localeBundle['queryAIQIP'], 
             query: "select * from EAM_AIQ_IP"], 
          resourceAlertsActiveButDisabled: [ 
             name: localeBundle['queryResourceAlertDefsActiveButDisabled'], 
             query: {conn -> "select id, name, description, resource_id from EAM_ALERT_DEFINITION where "+
                    "(parent_id is null or parent_id > 0) and active="+
                    DBUtil.getBooleanValue(true, conn)+" and enabled="+
                    DBUtil.getBooleanValue(false, conn)+" and deleted="+
                    DBUtil.getBooleanValue(false, conn)}],
        ]
        
        def res = [:]
        withConnection() { conn ->
            for (q in queries.keySet()) {
                def query = queries[q]
                if (!query.viewable || 
                    (query.viewable in Closure && query.viewable(conn))) 
                {
                    res[q] = query   
                }
            }
        }
        res
    }
    
    private h(str) {
        str.toString().toHtml()
    }
    
    private getJVMSupportsTraces() {
        def ver = System.getProperty('java.version')[0..2]
        ver != '1.3' && ver != '1.4' 
    }
    
    def runQuery(params) {
        def id    = params.getOne('query')
        
        def query
        
        if (databaseQueries[id].query in Closure) {
            query = withConnection() { conn -> 
                databaseQueries[id].query(conn)       
            }
        } else {
            query = databaseQueries[id].query        
        }    
        
        def name  = databaseQueries[id].name
        def start = now()

        log.info("Running query [${query}]")
        def res = withConnection() { conn ->
            def sql    = new Sql(conn)
            def output = new StringBuffer()
            def rowIdx = 0
            def md


            sql.eachRow(query) { rs ->
                if (rowIdx++ == 0) {
                    output << "<table cellspadding=3 cellspacing=0 border=0 width=98%><thead><tr>"
                    md = rs.getMetaData()
                    for (i in 1..md.columnCount) {
                        output <<  "<td>${h md.getColumnLabel(i)}</td>"
                    }
                    output << "</tr></thead><tbody>"
                }
                output << "<tr>"
                for (i in 0..<md.columnCount) {
                    def type = md.getColumnType(i + 1)
                    if (type in [Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY]) {
                        output << "<td>*binary*</td>"
                    } else {
                       def trimmedCol = h(rs[i].toString().trim())
                        if (trimmedCol.length() == 0) {
                        output << "<td>&nbsp;</td>"
                        } else {
                         output << "<td>"
                         output << trimmedCol
                         output << "</td>"
                       }
                    }
                }
                output << "</tr>"
            }
            output << "</tbody></table>"
            if (!rowIdx) {
                return localeBundle['noResultsFound']
            } else {
                return output
            }
        }
        
        def queryData = "${name} executed in ${now() - start} ms<br/><br/>"
        [ queryData: queryData + res ]
    }
}
