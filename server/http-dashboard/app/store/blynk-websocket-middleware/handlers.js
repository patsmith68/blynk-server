import {
  blynkVW,
  blynkWsResponse,
  blynkWsLogEvent,
  blynkWsDeviceConnect,
  blynkWsDeviceDisconnect,
} from './actions';

import {
  getTrackDeviceId,
  getTrackOnlyByDeviceId
} from './selectors';


const decodeBody = (dataView) => {
  const dec = new TextDecoder("utf-8");
  const body = dec.decode(new DataView(dataView.buffer, 3));

  return body;
};

export const Handlers = (params) => {

  const {store, options, action, command, msgId, dataView} = params;

  const responseHandler = ({ responseCode }) => {

    store.dispatch(blynkWsResponse({
      id      : msgId,
      response: {
        command     : command,
        responseCode: responseCode
      }
    }));

    if (options.isDebugMode)
      options.debug("blynkWsMessage Response", action, {
        command     : command,
        msgId       : msgId,
        responseCode: responseCode
      });
  };

  const hardwareHandler = ({ msgId }) => {

    // receive newly generated msgId because hardwareHandler has now own unique ID

    const trackDeviceId = getTrackDeviceId(store.getState());
    const trackOnlyByDeviceId = getTrackOnlyByDeviceId(store.getState());

    const body = decodeBody(dataView);

    const bodyArray = body.split('\0');

    const deviceId = bodyArray[0].replace('0-', '');
    const pin = bodyArray[2];
    const value = bodyArray[3];

    const deviceIdEqualTrackDeviceId = Number(trackDeviceId) === Number(deviceId);

    if (options.isDebugMode)
      options.debug("blynkWsMessage Hardware", action, {
        command     : command,
        msgId       : msgId,
        bodyArray: bodyArray,
        trackDeviceId,
        trackOnlyByDeviceId,
        deviceIdEqualTrackDeviceId
      });

    if(trackOnlyByDeviceId && !deviceIdEqualTrackDeviceId)
      return false;

    store.dispatch(blynkWsResponse({
      id      : msgId,
      response: {
        command: command,
        body   : bodyArray
      }
    }));

    store.dispatch(blynkVW({
      deviceId: Number(deviceId),
      pin: Number(pin),
      value: value
    }));
  };

  const logEventHandler = ({ msgId }) => {

    const body = decodeBody(dataView);

    const bodyArray = body.split('\0');

    const deviceId = bodyArray[0];
    const eventCode = bodyArray[1];

    if (options.isDebugMode)
      options.debug("blynkWsMessage LogEvent", action, {
        command     : command,
        msgId       : msgId,
        bodyArray: bodyArray,
        deviceId,
        eventCode
      });

    store.dispatch(blynkWsLogEvent({
      deviceId,
      eventCode,
    }));

  };

  const deviceConnectHandler = ({ msgId }) => {

    const body = decodeBody(dataView);

    const bodyArray = body.split('\0');

    const deviceId = bodyArray[0].replace('0-', '');

    if (options.isDebugMode)
      options.debug("blynkWsMessage DeviceConnect", action, {
        command     : command,
        msgId       : msgId,
        bodyArray: bodyArray,
        deviceId
      });

    store.dispatch(blynkWsDeviceConnect({
      deviceId
    }));

  };

  const deviceDisconnectHandler = ({ msgId }) => {

    const body = decodeBody(dataView);

    const bodyArray = body.split('\0');

    const deviceId = bodyArray[0].replace('0-', '');

    if (options.isDebugMode)
      options.debug("blynkWsMessage DeviceDisconnect", action, {
        command     : command,
        msgId       : msgId,
        bodyArray: bodyArray,
        deviceId
      });

    store.dispatch(blynkWsDeviceDisconnect({
      deviceId
    }));

  };


  const appSyncHandler = ({ msgId }) => {

    const body = decodeBody(dataView);

    const bodyArray = body.split('\0');

    if (options.isDebugMode)
      options.debug("blynkWsMessage AppSync", action, {
        command     : command,
        msgId       : msgId,
        bodyArray: bodyArray
      });

    store.dispatch(blynkWsResponse({
      id      : msgId,
      response: {
        command: command,
        body   : bodyArray
      }
    }));

    const deviceId = bodyArray[0];
    const pin = bodyArray[2];
    const value = bodyArray[3];

    store.dispatch(blynkVW({
      deviceId: Number(deviceId),
      pin: Number(pin),
      value: value
    }));

  };

  const unknownCommandHandler = () => {

    if (options.isDebugMode)
      options.debug("blynkWsMessage Unknown", action, {
        command     : command,
        msgId       : msgId,
      });

    store.dispatch(blynkWsResponse({
      id      : msgId,
      response: {
        command: command
      }
    }));
  };


  return {
    ResponseHandler: responseHandler,
    HardwareHandler: hardwareHandler,
    LogEventHandler: logEventHandler,
    DeviceConnectHandler: deviceConnectHandler,
    DeviceDisconnectHandler: deviceDisconnectHandler,
    AppSyncHandler: appSyncHandler,
    UnknownCommandHandler: unknownCommandHandler,
  };
};
