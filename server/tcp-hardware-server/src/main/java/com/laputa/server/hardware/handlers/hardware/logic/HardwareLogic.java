package com.laputa.server.hardware.handlers.hardware.logic;

import com.laputa.server.Holder;
import com.laputa.server.core.dao.ReportingDao;
import com.laputa.server.core.dao.SessionDao;
import com.laputa.server.core.model.DashBoard;
import com.laputa.server.core.model.auth.Session;
import com.laputa.server.core.model.enums.PinType;
import com.laputa.server.core.processors.EventorProcessor;
import com.laputa.server.core.processors.WebhookProcessor;
import com.laputa.server.core.protocol.model.messages.StringMessage;
import com.laputa.server.core.session.HardwareStateHolder;
import com.laputa.utils.ParseUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.laputa.server.core.protocol.enums.Command.HARDWARE;
import static com.laputa.utils.LaputaByteBufUtil.illegalCommand;
import static com.laputa.utils.StringUtils.split3;

/**
 * Handler responsible for forwarding messages from hardware to applications.
 * Also handler stores all incoming hardware commands to disk in order to export and
 * analyze data.
 *
 * The Laputa Project.
 * Created by Sommer
 * Created on 2/1/2015.
 *
 */
public class HardwareLogic {

    private static final Logger log = LogManager.getLogger(HardwareLogic.class);

    private final ReportingDao reportingDao;
    private final SessionDao sessionDao;
    private final EventorProcessor eventorProcessor;
    private final WebhookProcessor webhookProcessor;

    public HardwareLogic(Holder holder, String email) {
        this.sessionDao = holder.sessionDao;
        this.reportingDao = holder.reportingDao;
        this.eventorProcessor = holder.eventorProcessor;
        this.webhookProcessor = new WebhookProcessor(holder.asyncHttpClient,
                holder.limits.WEBHOOK_PERIOD_LIMITATION,
                holder.limits.WEBHOOK_RESPONSE_SUZE_LIMIT_BYTES,
                holder.limits.WEBHOOK_FAILURE_LIMIT,
                holder.stats,
                email);
    }

    private static boolean isWriteOperation(String body) {
        return body.charAt(1) == 'w';
    }

    public void messageReceived(ChannelHandlerContext ctx, HardwareStateHolder state, StringMessage message) {
        Session session = sessionDao.userSession.get(state.userKey);

        final String body = message.body;

        //minimum command - "ar 1"
        if (body.length() < 4) {
            log.debug("HardwareLogic command body too short.");
            ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
            return;
        }

        final int dashId = state.dashId;
        final int deviceId = state.deviceId;

        DashBoard dash = state.user.profile.getDashByIdOrThrow(dashId);

        if (isWriteOperation(body)) {
            String[] splitBody = split3(body);

            if (splitBody.length < 3 || splitBody[0].length() == 0 || splitBody[2].length() == 0) {
                log.debug("Write command is wrong.");
                ctx.writeAndFlush(illegalCommand(message.id), ctx.voidPromise());
                return;
            }

            final PinType pinType = PinType.getPinType(splitBody[0].charAt(0));
            final byte pin = ParseUtil.parseByte(splitBody[1]);
            final String value = splitBody[2];
            final long now = System.currentTimeMillis();

            reportingDao.process(state.user, dashId, deviceId, pin, pinType, value, now);
            dash.update(deviceId, pin, pinType, value, now);

            process(dash, deviceId, session, pin, pinType, value, now);
        }

        if (dash.isActive) {
            session.sendToApps(HARDWARE, message.id, dashId, deviceId, body);
        } else {
            log.debug("No active dashboard.");
        }
    }

    private void process(DashBoard dash, int deviceId, Session session, byte pin, PinType pinType, String value, long now) {
        try {
            eventorProcessor.process(session, dash, deviceId, pin, pinType, value, now);
            webhookProcessor.process(session, dash, deviceId, pin, pinType, value, now);
        } catch (Exception e) {
            log.error("Error processing eventor/webhook.", e);
        }
    }

}
