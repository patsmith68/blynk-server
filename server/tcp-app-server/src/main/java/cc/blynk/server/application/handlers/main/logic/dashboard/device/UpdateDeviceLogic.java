package cc.blynk.server.application.handlers.main.logic.dashboard.device;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandBodyException;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.internal.ParseUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.internal.BlynkByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public final class UpdateDeviceLogic {

    private static final Logger log = LogManager.getLogger(UpdateDeviceLogic.class);

    private UpdateDeviceLogic() {
    }

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = ParseUtil.parseInt(split[0]);
        String deviceString = split[1];

        if (deviceString == null || deviceString.isEmpty()) {
            throw new IllegalCommandException("Income device message is empty.");
        }

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        Device newDevice = JsonParser.parseDevice(deviceString);

        log.debug("Updating new device {}.", deviceString);

        if (newDevice.isNotValid()) {
            throw new IllegalCommandException("Income device message is not valid.");
        }

        Device existingDevice = dash.getDeviceById(newDevice.id);

        if (existingDevice == null) {
            throw new IllegalCommandBodyException("Attempt to update device with non existing id.");
        }

        existingDevice.update(newDevice);

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

}
