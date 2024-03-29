package xyz.chener.zp.zpusermodule.ws;

import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import xyz.chener.zp.common.config.ctx.ApplicationContextHolder;
import xyz.chener.zp.common.entity.LoginUserDetails;
import xyz.chener.zp.common.utils.Jwt;
import xyz.chener.zp.common.utils.ObjectUtils;
import xyz.chener.zp.zpusermodule.service.QrCodeLoginService;
import xyz.chener.zp.zpusermodule.ws.entity.WsClient;
import xyz.chener.zp.zpusermodule.ws.entity.WsMessage;
import xyz.chener.zp.zpusermodule.ws.entity.WsMessageConstVar;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
public class WsMessageProcesser {

    public static void checkMessageLegal(WsMessage message, Session session) {
        if (message.getJwt() == null) {
            WsConnector.close(session);
            return;
        }
        Jwt jwt = ApplicationContextHolder.getApplicationContext().getBean(Jwt.class);
        LoginUserDetails u = jwt.decode(message.getJwt());
        if (u == null) {
            WsConnector.close(session);
            return;
        }
        message.setUsername(u.getUsername());
        message.setObj(u);
    }

    public static void heartBeat(WsMessage message, Session session) {
        WsClient unAuthConnect = WsCache.getUnAuthConnect(session.getId());
        if (unAuthConnect != null) {
            WsCache.unAuthConnect.invalidate(session.getId());
            unAuthConnect.setUsername(message.getUsername());
        }else {
            unAuthConnect = WsCache.getAuthConnect(session.getId());
        }
        if (unAuthConnect == null){
            log.error("心跳包异常，未找到连接信息");
            return;
        }
        if (message.getObj() != null && message.getObj() instanceof LoginUserDetails lud){
            unAuthConnect.setSystem(lud.getSystem());
        }
        WsCache.putAuthConnect(session.getId(), unAuthConnect);
        WsConnector.sendObject(WsMessage.builder()
                .code(message.getCode()).username(message.getUsername()).message("heart beat success").build()
                , session.getId());
    }

    public static void qrCodeLogin(WsMessage message, Session session){
        WsClient unAuthConnect = WsCache.getUnAuthConnect(session.getId());
        if (unAuthConnect != null) {
            String uuid = UUID.randomUUID().toString();
            unAuthConnect.setUsername(uuid);
            QrCodeLoginService qrCodeLoginService = ApplicationContextHolder.getApplicationContext().getBean(QrCodeLoginService.class);
            WsMessage msg = new WsMessage();
            if (!qrCodeLoginService.putQrCodeLogin(uuid,session.getId(),unAuthConnect.getIp(),message.getMessage())) {
                msg.setCode(WsMessageConstVar.QRCODE_LOGIN_FAIL);
                WsConnector.sendObject(msg,session.getId());
                return;
            }
            msg.setCode(WsMessageConstVar.QRCODE_LOGIN_RESPONSE);
            msg.setMessage(uuid);
            WsCache.unAuthConnect.invalidate(session.getId());
            WsCache.qrCodeLoginConnect.put(session.getId(), unAuthConnect);
            WsConnector.sendObject(msg,session.getId());
        }
    }

    public static void sendAll(WsMessage message) {
        WsCache.getAllAuthConnect().forEach((wsClient) -> {
            WsConnector.sendObject(message, wsClient.getSessionId());
        });
    }

    public static boolean sendUser(WsMessage message, String username) {
        AtomicBoolean rt = new AtomicBoolean(false);

        WsCache.getAllAuthConnect().forEach((wsClient) -> {
            if (ObjectUtils.nullSafeEquals(wsClient.getUsername(), username)) {
                WsConnector.sendObject(message, wsClient.getSessionId());
                rt.set(true);
            }
        });
        return rt.get();
    }


}
