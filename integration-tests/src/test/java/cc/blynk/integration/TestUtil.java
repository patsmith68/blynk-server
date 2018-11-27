package cc.blynk.integration;

import cc.blynk.integration.model.SimpleClientHandler;
import cc.blynk.integration.model.tcp.ClientPair;
import cc.blynk.integration.model.tcp.TestAppClient;
import cc.blynk.integration.model.tcp.TestHardClient;
import cc.blynk.integration.model.websocket.AppWebSocketClient;
import cc.blynk.server.Holder;
import cc.blynk.server.core.BlockingIOProcessor;
import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.Profile;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.device.BoardType;
import cc.blynk.server.core.model.device.Device;
import cc.blynk.server.core.model.device.Tag;
import cc.blynk.server.core.model.dto.ProductDTO;
import cc.blynk.server.core.model.enums.PinType;
import cc.blynk.server.core.model.enums.SortOrder;
import cc.blynk.server.core.model.permissions.Role;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.web.Organization;
import cc.blynk.server.core.model.web.product.MetaField;
import cc.blynk.server.core.model.web.product.Product;
import cc.blynk.server.core.model.web.product.WebDashboard;
import cc.blynk.server.core.model.web.product.metafields.DeviceOwnerMetaField;
import cc.blynk.server.core.model.widgets.MobileSyncWidget;
import cc.blynk.server.core.model.widgets.web.WebLineGraph;
import cc.blynk.server.core.model.widgets.web.WebSlider;
import cc.blynk.server.core.model.widgets.web.WebSource;
import cc.blynk.server.core.model.widgets.web.WebSwitch;
import cc.blynk.server.core.model.widgets.web.WebWidget;
import cc.blynk.server.core.model.widgets.web.label.WebLabel;
import cc.blynk.server.core.protocol.model.messages.MessageBase;
import cc.blynk.server.core.protocol.model.messages.ResponseMessage;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.protocol.model.messages.WebJsonMessage;
import cc.blynk.server.core.protocol.model.messages.appllication.GetServerMessage;
import cc.blynk.server.core.protocol.model.messages.common.HardwareMessage;
import cc.blynk.server.core.protocol.model.messages.hardware.HardwareLogEventMessage;
import cc.blynk.server.notifications.mail.MailWrapper;
import cc.blynk.server.notifications.push.GCMWrapper;
import cc.blynk.server.notifications.sms.SMSWrapper;
import cc.blynk.server.notifications.twitter.TwitterWrapper;
import cc.blynk.utils.AppNameUtil;
import cc.blynk.utils.StringUtils;
import cc.blynk.utils.properties.ServerProperties;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.mockito.ArgumentCaptor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static cc.blynk.integration.APIBaseTest.createDeviceNameMeta;
import static cc.blynk.integration.APIBaseTest.createDeviceOwnerMeta;
import static cc.blynk.server.core.model.web.Organization.NO_PARENT_ID;
import static cc.blynk.server.core.model.widgets.outputs.graph.AggregationFunctionType.RAW_DATA;
import static cc.blynk.server.core.protocol.enums.Command.BLYNK_INTERNAL;
import static cc.blynk.server.core.protocol.enums.Command.BRIDGE;
import static cc.blynk.server.core.protocol.enums.Command.CONNECT_REDIRECT;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_DEVICE;
import static cc.blynk.server.core.protocol.enums.Command.CREATE_TAG;
import static cc.blynk.server.core.protocol.enums.Command.DEVICE_CONNECTED;
import static cc.blynk.server.core.protocol.enums.Command.DEVICE_DISCONNECTED;
import static cc.blynk.server.core.protocol.enums.Command.DEVICE_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_CLONE_CODE;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROJECT_BY_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.GET_PROVISION_TOKEN;
import static cc.blynk.server.core.protocol.enums.Command.LOAD_PROFILE_GZIPPED;
import static cc.blynk.server.core.protocol.enums.Command.OUTDATED_APP_NOTIFICATION;
import static cc.blynk.server.core.protocol.enums.Command.SET_WIDGET_PROPERTY;
import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND;
import static cc.blynk.server.core.protocol.enums.Response.ILLEGAL_COMMAND_BODY;
import static cc.blynk.server.core.protocol.enums.Response.INVALID_TOKEN;
import static cc.blynk.server.core.protocol.enums.Response.NOT_ALLOWED;
import static cc.blynk.server.core.protocol.enums.Response.OK;
import static cc.blynk.server.core.protocol.enums.Response.SERVER_ERROR;
import static cc.blynk.utils.StringUtils.WEBSOCKET_WEB_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

public final class TestUtil {

    private static final ObjectReader profileReader = JsonParser.init().readerFor(Profile.class);

    private TestUtil() {
    }

    public static String getBody(SimpleClientHandler responseMock) throws Exception {
        return getBody(responseMock, 1);
    }

    public static String getBody(SimpleClientHandler responseMock, int expectedMessageOrder) throws Exception {
        ArgumentCaptor<MessageBase> objectArgumentCaptor = ArgumentCaptor.forClass(MessageBase.class);
        verify(responseMock, timeout(1000).times(expectedMessageOrder)).channelRead(any(), objectArgumentCaptor.capture());
        List<MessageBase> arguments = objectArgumentCaptor.getAllValues();
        MessageBase messageBase = arguments.get(expectedMessageOrder - 1);
        if (messageBase instanceof StringMessage) {
            return ((StringMessage) messageBase).body;
        } else if (messageBase.command == LOAD_PROFILE_GZIPPED
                || messageBase.command == GET_PROJECT_BY_TOKEN
                || messageBase.command == GET_PROVISION_TOKEN
                || messageBase.command == GET_PROJECT_BY_CLONE_CODE) {
            return new String(BaseTest.decompress(messageBase.getBytes()));
        }

        try {
            throw new RuntimeException("Unexpected message");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unexpected message");
        }
    }

    public static Profile parseProfile(InputStream reader) throws Exception {
        return profileReader.readValue(reader);
    }

    public static Profile parseProfile(String reader) throws Exception {
        return profileReader.readValue(reader);
    }

    public static String readTestUserProfile(String fileName) throws Exception{
        InputStream is = TestUtil.class.getResourceAsStream("/json_test/" + fileName);
        Profile profile = parseProfile(is);
        return profile.toString();
    }

    public static String readTestUserProfile() throws Exception {
        return readTestUserProfile("user_profile_json.txt");
    }

    public static void saveProfile(TestAppClient appClient, DashBoard... dashBoards) {
        for (DashBoard dash : dashBoards) {
            appClient.createDash(dash);
        }
    }

    public static String b(String body) {
        return body.replaceAll(" ", StringUtils.BODY_SEPARATOR_STRING);
    }

    public static ResponseMessage illegalCommand(int msgId) {
        return new ResponseMessage(msgId, ILLEGAL_COMMAND);
    }

    public static ResponseMessage illegalCommandBody(int msgId) {
        return new ResponseMessage(msgId, ILLEGAL_COMMAND_BODY);
    }

    public static ResponseMessage ok(int msgId) {
        return new ResponseMessage(msgId, OK);
    }

    public static StringMessage bridge(int msgId, String body) {
        return new StringMessage(msgId, BRIDGE, b(body));
    }

    public static StringMessage internal(int msgId, String body) {
        return new StringMessage(msgId, BLYNK_INTERNAL, b(body));
    }

    public static HardwareLogEventMessage logEvent(int msgId, String body) {
        return new HardwareLogEventMessage(msgId, b(body));
    }

    public static StringMessage deviceConnected(int msgId, int deviceId) {
        return new StringMessage(msgId, DEVICE_CONNECTED, "" + deviceId);
    }

    public static StringMessage deviceConnected(int msgId, String body) {
        return new StringMessage(msgId, DEVICE_CONNECTED, body);
    }

    public static GetServerMessage getServer(int msgId, String body) {
        return new GetServerMessage(msgId, body);
    }

    public static StringMessage deviceOffline(int msgId, int deviceId) {
        return new StringMessage(msgId, DEVICE_DISCONNECTED, String.valueOf(deviceId));
    }

    public static StringMessage createTag(int msgId, Tag tag) {
        return createTag(msgId, tag.toString());
    }

    public static StringMessage createTag(int msgId, String body) {
        return new StringMessage(msgId, CREATE_TAG, body);
    }

    public static StringMessage appIsOutdated(int msgId, String body) {
        return new StringMessage(msgId, OUTDATED_APP_NOTIFICATION, body);
    }

    public static StringMessage appSync(int msgId, String body) {
        return new StringMessage(msgId, DEVICE_SYNC, b(body));
    }

    public static StringMessage hardware(int msgId, String body) {
        return new HardwareMessage(msgId, b(body));
    }

    public static StringMessage appSync(String body) {
        return appSync(MobileSyncWidget.SYNC_DEFAULT_MESSAGE_ID, body);
    }

    public static StringMessage setProperty(int msgId, String body) {
        return new StringMessage(msgId, SET_WIDGET_PROPERTY, b(body));
    }

    public static MessageBase webJson(int msgId, String message) {
        return new WebJsonMessage(msgId, WebJsonMessage.toJson(message, 0));
    }

    public static MessageBase webJson(int msgId, String message, int code) {
        return new WebJsonMessage(msgId, WebJsonMessage.toJson(message, code));
    }

    public static StringMessage createDevice(int msgId, String body) {
        return new StringMessage(msgId, CREATE_DEVICE, body);
    }

    public static StringMessage createDevice(int msgId, Device device) {
        return createDevice(msgId, device.toString());
    }

    public static StringMessage connectRedirect(int msgId, String body) {
        return new StringMessage(msgId, CONNECT_REDIRECT, b(body));
    }

    public static ResponseMessage serverError(int msgId) {
        return new ResponseMessage(msgId, SERVER_ERROR);
    }

    public static ResponseMessage notAllowed(int msgId) {
        return new ResponseMessage(msgId, NOT_ALLOWED);
    }

    public static ResponseMessage invalidToken(int msgId) {
        return new ResponseMessage(msgId, INVALID_TOKEN);
    }

    public static ClientPair initAppAndHardPair(String host, int appPort, int hardPort,
                                                String user, String pass,
                                                String jsonProfile,
                                                ServerProperties properties, int energy) throws Exception {

        TestAppClient appClient = new TestAppClient(host, appPort, properties);
        TestHardClient hardClient = new TestHardClient(host, hardPort);

        return initAppAndHardPair(appClient, hardClient, user, pass, jsonProfile, energy);
    }

    public static ClientPair initAppAndHardPair(TestAppClient appClient, TestHardClient hardClient,
                                                String user, String pass,
                                                String jsonProfile, int energy) throws Exception {

        appClient.start();
        hardClient.start();

        String userProfileString = readTestUserProfile(jsonProfile);
        Profile profile = parseProfile(userProfileString);
        int dashId = profile.dashBoards[0].id;

        appClient.register(user, pass, AppNameUtil.BLYNK);
        appClient.login(user, pass, "Android", "2.27.0");
        int rand = ThreadLocalRandom.current().nextInt();
        appClient.send("addEnergy " + energy + "\0" + String.valueOf(rand));
        //we should wait until login finished. Only after that we can send commands
        verify(appClient.responseMock, timeout(1000)).channelRead(any(), eq(ok(2)));

        saveProfile(appClient, profile.dashBoards);

        appClient.activate(dashId);

        ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(appClient.responseMock, timeout(2000).times(4 + profile.dashBoards.length)).channelRead(any(), objectArgumentCaptor.capture());

        Device device = new Device();
        device.name = "Default Device";
        appClient.createDevice(device);
        Device createdDevice = appClient.parseDevice(5 + profile.dashBoards.length);

        DeviceOwnerMetaField deviceOwnerMetaField = (DeviceOwnerMetaField) createdDevice.metaFields[1];
        appClient.updateDeviceMetafield(createdDevice.id,
                createDeviceOwnerMeta(deviceOwnerMetaField.id, deviceOwnerMetaField.name, user, true));
        //appClient.verifyResult(ok(6 + profile.dashBoards.length + expectedSyncCommandsCount));

        appClient.getDevices();
        Device[] devices = appClient.parseDevices(7 + profile.dashBoards.length );
        Device latestOne = devices[devices.length - 1];
        String token = latestOne.token;

        hardClient.login(token);
        verify(hardClient.responseMock, timeout(2000)).channelRead(any(), eq(ok(1)));
        //verify(appClient.responseMock, timeout(2000)).channelRead(any(), eq(deviceConnected(1, "" + dashId + "-0")));

        appClient.reset();
        hardClient.reset();

        return new ClientPair(appClient, hardClient, token, latestOne.id);
    }

    public static String getDataFolder() {
        try {
            return Files.createTempDirectory("blynk_test_", new FileAttribute[0]).toString();
        } catch (IOException e) {
            throw new RuntimeException("Unable create temp dir.", e);
        }
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            //we can ignore it
        }
    }

    @SuppressWarnings("unchecked")
    public static List<String> consumeJsonPinValues(String response) {
        return JsonParser.readAny(response, List.class);
    }

    @SuppressWarnings("unchecked")
    public static List<String> consumeJsonPinValues(CloseableHttpResponse response) throws IOException {
        return JsonParser.readAny(consumeText(response), List.class);
    }

    @SuppressWarnings("unchecked")
    public static String consumeText(CloseableHttpResponse response) throws IOException {
        return EntityUtils.toString(response.getEntity());
    }

    public static SSLContext initUnsecuredSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
        X509TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) {

            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] x509Certificates, String s) {

            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{ tm }, null);

        return context;
    }

    public static Holder createHolderWithIOMock(ServerProperties serverProperties,
                                                String dbFileName) {
        return new Holder(serverProperties,
                mock(TwitterWrapper.class),
                mock(MailWrapper.class),
                mock(GCMWrapper.class),
                mock(SMSWrapper.class),
                mock(BlockingIOProcessor.class),
                dbFileName);
    }

    public static Holder createDefaultHolder(ServerProperties serverProperties,
                                             String dbFileName) {
        return new Holder(serverProperties,
                mock(TwitterWrapper.class),
                mock(MailWrapper.class),
                mock(GCMWrapper.class),
                mock(SMSWrapper.class),
                new BlockingIOProcessor(
                        serverProperties.getIntProperty("blocking.processor.thread.pool.limit", 1),
                        serverProperties.getIntProperty("notifications.queue.limit", 100)
                ), dbFileName);
    }

    private static final int DEFAULT_TEST_HTTPS_PORT = 10443;

    public static AppWebSocketClient loggedDefaultClient(String username, String pass) throws Exception {
        AppWebSocketClient appWebSocketClient = defaultClient();
        appWebSocketClient.start();
        appWebSocketClient.login(username, pass);
        appWebSocketClient.verifyResult(ok(1));
        appWebSocketClient.reset();
        return appWebSocketClient;
    }

    public static AppWebSocketClient loggedDefaultClient(User user) throws Exception {
        AppWebSocketClient appWebSocketClient = defaultClient();
        appWebSocketClient.start();
        appWebSocketClient.login(user);
        appWebSocketClient.verifyResult(ok(1));
        appWebSocketClient.reset();
        return appWebSocketClient;
    }

    public static AppWebSocketClient defaultClient() throws Exception {
        return new AppWebSocketClient("localhost", DEFAULT_TEST_HTTPS_PORT, WEBSOCKET_WEB_PATH);
    }

    public static CloseableHttpClient getDefaultHttpsClient() throws Exception {
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(initUnsecuredSSLContext(), new MyHostVerifier());
        return HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .build();
    }

    public static WebLabel createWebLabelWidget(int id, String label) {
        return createWebLabelWidget(id, label, 1);
    }

    public static WebLabel createWebLabelWidget(int id, String label, int pin) {
        WebLabel webLabel = new WebLabel();
        webLabel.id = id;
        webLabel.label = label;
        webLabel.x = 1;
        webLabel.y = 2;
        webLabel.height = 10;
        webLabel.width = 20;
        webLabel.sources = new WebSource[] {
                new WebSource("Web Source Label", "#334455",
                        false, RAW_DATA, new DataStream((byte) pin, PinType.VIRTUAL),
                        null,
                        null,
                        null, SortOrder.ASC, 10, false, null, false)
        };
        return webLabel;
    }

    public static WebSwitch createWebSwitchWidget(int id, String onLabel, int pin) {
        WebSwitch webSwitch = new WebSwitch();
        webSwitch.id = id;
        webSwitch.onLabel = onLabel;
        webSwitch.x = 3;
        webSwitch.y = 4;
        webSwitch.height = 50;
        webSwitch.width = 60;
        webSwitch.sources = new WebSource[] {
                new WebSource("Web Source Label", "#334455",
                        false, RAW_DATA, new DataStream((byte) pin, PinType.VIRTUAL),
                        null,
                        null,
                        null, SortOrder.ASC, 10, false, null, false)
        };
        return webSwitch;
    }

    public static WebSlider createWebSliderWidget(int id, String label, int pin) {
        WebSlider webSlider = new WebSlider();
        webSlider.id = id;
        webSlider.label = label;
        webSlider.x = 3;
        webSlider.y = 4;
        webSlider.height = 50;
        webSlider.width = 60;
        webSlider.sources = new WebSource[] {
                new WebSource("Web Source Label", "#334455",
                        false, RAW_DATA, new DataStream((byte) pin, PinType.VIRTUAL),
                        null,
                        null,
                        null, SortOrder.ASC, 10, false, null, false)
        };
        return webSlider;
    }

    public static WebLineGraph createWebLineGraph(int id, String label) {
        return createWebLineGraph(id, label, 1);
    }

    public static WebLineGraph createWebLineGraph(int id, String label, int pin) {
        WebLineGraph webGraph = new WebLineGraph();
        webGraph.id = id;
        webGraph.label = label;
        webGraph.x = 3;
        webGraph.y = 4;
        webGraph.height = 10;
        webGraph.width = 20;
        webGraph.sources = new WebSource[] {
                new WebSource("Web Source Label", "#334455", false,
                        RAW_DATA, new DataStream((byte) pin, PinType.VIRTUAL),
                        null,
                        null,
                        null,
                        SortOrder.ASC, 10, false, null, false)
        };
        return webGraph;
    }

    public static Organization createDefaultOrg() {
        Organization org = new Organization("Blynk Inc.", "Europe/Kiev", "/static/logo.png", true, NO_PARENT_ID,
                new Role(Role.SUPER_ADMIN_ROLE_ID, "Super Admin", 0b11111111111111111111111111111111, 0),
                new Role(1, "Admin", 0b11111111111111111111111111111111, 0),
                new Role(2, "Staff", 0b11111111111111111111111111111111, 0),
                new Role(3, "User", 0b11111111111111111111111111111111, 0)
        );
        Product product = new Product();
        product.name = "Default Product";
        product.boardType = BoardType.ESP8266.name();
        product.metaFields = new MetaField[] {
                createDeviceNameMeta(1, "Device Name", "My Default device Name", true),
                createDeviceOwnerMeta(2, "Device Owner", null, true),
        };
        org.products = new Product[] {
                product
        };
        return org;
    }

    public static ProductDTO updateProductName(ProductDTO productDTO, String name) {
        Product product = productDTO.toProduct();
        product.name = name;
        return new ProductDTO(product);
    }

    public static ProductDTO updateProductWebDash(ProductDTO productDTO, WebWidget... widgets) {
        Product product = productDTO.toProduct();
        product.webDashboard = new WebDashboard(
            widgets
        );
        return new ProductDTO(product);
    }

    public static ProductDTO updateProductMetafields(ProductDTO productDTO, MetaField... metaFields) {
        Product product = productDTO.toProduct();
        product.metaFields = metaFields;
        return new ProductDTO(product);
    }
}
