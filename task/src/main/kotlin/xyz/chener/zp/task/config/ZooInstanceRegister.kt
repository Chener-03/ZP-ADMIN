package xyz.chener.zp.task.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.shardingsphere.elasticjob.infra.env.IpUtils
import org.apache.shardingsphere.elasticjob.infra.handler.sharding.JobInstance
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.WatchedEvent
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.Watcher.Event
import org.apache.zookeeper.data.Stat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.cloud.endpoint.RefreshEndpoint
import org.springframework.cloud.endpoint.event.RefreshEvent
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import xyz.chener.zp.common.config.InfoRegistration
import xyz.chener.zp.common.config.ctx.ApplicationContextHolder
import xyz.chener.zp.task.core.ZookeeperProxy
import xyz.chener.zp.task.entity.ZooInstance
import kotlin.system.exitProcess


@Configuration
class ZooInstanceRegister {

    private val log : Logger = LoggerFactory.getLogger(ZooInstanceRegister::class.java)

    @Value("\${server.port}")
    var port : Int = 0


    @Bean
    fun zooInstance(taskConfiguration: TaskConfiguration): ZooInstance {
        taskConfiguration.taskCfg.registIp?.let{
            System.setProperty("elasticjob.preferred.network.ip",it)
        }

        val ipaddr = "${IpUtils.getIp()}:$port"
        return ZooInstance(
            IpUtils.getIp(),
            ipaddr,
            System.currentTimeMillis(),
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().totalMemory(),
            System.getProperty(InfoRegistration.APP_UID),
            JobInstance().jobInstanceId
        )
    }



    @Bean("zookeeperProxy")
    @RefreshScope
    fun zookeeperPorxy(taskConfiguration: TaskConfiguration,zooInstance: ZooInstance) : ZookeeperProxy {
        try {
            val zk = ZookeeperProxy(taskConfiguration.zk.address, taskConfiguration.zk.connectTimeOut, object : Watcher {
                override fun process(event: WatchedEvent?) {
                    if (event?.state == Event.KeeperState.Disconnected || event?.state == Event.KeeperState.Expired) {
                        ApplicationContextHolder.getApplicationContext().publishEvent(
                            RefreshEvent(
                                this,
                                null,
                                "Zookeeper Refresh"
                            )
                        )
                        ApplicationContextHolder.getApplicationContext().getBean(ZookeeperProxy::class.java).getRootDir()
                    }
                }
            }, taskConfiguration)
            val exists: Stat? = zk.exists("${zk.getRootDir()}/${zooInstance.address}", false)
            if (exists == null){
                val path = zk.create("${zk.getRootDir()}/${zooInstance.address}", ObjectMapper().writeValueAsBytes(zooInstance), zk.getAcl(), CreateMode.EPHEMERAL)
                log.info("注册实例:{}",path)
            }
            return zk
        }catch (e:Exception) {
            log.error("zookeeper初始化失败", e)
            val applicationContext = ApplicationContextHolder.getApplicationContext() as ConfigurableApplicationContext?
            applicationContext?.close()
            exitProcess(0)
        }
    }




}