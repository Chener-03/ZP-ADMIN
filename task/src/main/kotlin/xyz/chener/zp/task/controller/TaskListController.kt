package xyz.chener.zp.task.controller

import com.alibaba.cloud.nacos.NacosDiscoveryProperties
import com.alibaba.cloud.nacos.NacosServiceManager
import com.alibaba.cloud.nacos.endpoint.NacosDiscoveryEndpoint
import com.alibaba.nacos.api.naming.NamingFactory
import org.apache.shardingsphere.elasticjob.api.JobConfiguration
import org.apache.shardingsphere.elasticjob.lite.api.bootstrap.impl.ScheduleJobBootstrap
import org.apache.shardingsphere.elasticjob.lite.lifecycle.internal.operate.JobOperateAPIImpl
import org.apache.shardingsphere.elasticjob.lite.lifecycle.internal.statistics.ShardingStatisticsAPIImpl
import org.apache.shardingsphere.elasticjob.reg.zookeeper.ZookeeperRegistryCenter
import org.apache.shardingsphere.elasticjob.tracing.api.TracingConfiguration
import org.apache.zookeeper.ZooKeeper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.chener.zp.common.config.ctx.ApplicationContextHolder
import xyz.chener.zp.common.config.unifiedReturn.annotation.UnifiedReturn
import xyz.chener.zp.common.entity.WriteList
import xyz.chener.zp.common.utils.NacosUtils
import xyz.chener.zp.common.utils.SecurityUtils
import xyz.chener.zp.task.core.jobs.TestSimpleJob
import xyz.chener.zp.task.core.listener.TaskExecContextListener
import xyz.chener.zp.task.entity.ZooInstance
import xyz.chener.zp.task.service.InstanceService
import javax.sql.DataSource


@RestController
@RequestMapping("/api/web/task")
@UnifiedReturn
@Validated
open class TaskListController {


    @Autowired
    private lateinit var zookeeperRegistryCenter: ZookeeperRegistryCenter

    @Autowired
    lateinit var nacosUtils: NacosUtils

    @Autowired
    lateinit var instanceService: InstanceService

    @RequestMapping("/test")
    @WriteList
    fun test():Any{
        val shardingInfo = ShardingStatisticsAPIImpl(zookeeperRegistryCenter).getShardingInfo("test12")
        JobOperateAPIImpl(zookeeperRegistryCenter).shutdown("test12",null)
        return shardingInfo
    }

    @RequestMapping("/test1")
    @WriteList
    fun test1():Any{
        val jobConfiguration = JobConfiguration
            .newBuilder("test12", 2)
            .cron("0/10 * * * * ?")
            .jobParameter("123")
            .addExtraConfigurations(TracingConfiguration("RDB",ApplicationContextHolder.getApplicationContext().getBean(DataSource::class.java)))
            .description("测试任务")
            .overwrite(true)
            .disabled(true)
            .misfire(false)
            .jobErrorHandlerType("IGNORE")
            .jobListenerTypes(TaskExecContextListener::class.java.name)
            .build()
        val job = TestSimpleJob()
        val jb = ScheduleJobBootstrap(zookeeperRegistryCenter, job, jobConfiguration)
        return 1
    }


    @GetMapping("/getOnlineInstance")
    @WriteList
    open fun getOnlineInstance():List<ZooInstance>{
        return instanceService.getOnlineInstance()
    }


}