package android.audio.policy.configuration.V7_0;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.datatype.DatatypeConfigurationException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class Modules {
    private List<Module> module;

    /* loaded from: classes.dex */
    public static class Module {
        private AttachedDevices attachedDevices;
        private String defaultOutputDevice;
        private DevicePorts devicePorts;
        private HalVersion halVersion;
        private MixPorts mixPorts;
        private String name;
        private Routes routes;

        public AttachedDevices getAttachedDevices() {
            return this.attachedDevices;
        }

        boolean hasAttachedDevices() {
            if (this.attachedDevices == null) {
                return false;
            }
            return true;
        }

        public void setAttachedDevices(AttachedDevices attachedDevices) {
            this.attachedDevices = attachedDevices;
        }

        public String getDefaultOutputDevice() {
            return this.defaultOutputDevice;
        }

        boolean hasDefaultOutputDevice() {
            if (this.defaultOutputDevice == null) {
                return false;
            }
            return true;
        }

        public void setDefaultOutputDevice(String defaultOutputDevice) {
            this.defaultOutputDevice = defaultOutputDevice;
        }

        public MixPorts getMixPorts() {
            return this.mixPorts;
        }

        boolean hasMixPorts() {
            if (this.mixPorts == null) {
                return false;
            }
            return true;
        }

        public void setMixPorts(MixPorts mixPorts) {
            this.mixPorts = mixPorts;
        }

        public DevicePorts getDevicePorts() {
            return this.devicePorts;
        }

        boolean hasDevicePorts() {
            if (this.devicePorts == null) {
                return false;
            }
            return true;
        }

        public void setDevicePorts(DevicePorts devicePorts) {
            this.devicePorts = devicePorts;
        }

        public Routes getRoutes() {
            return this.routes;
        }

        boolean hasRoutes() {
            if (this.routes == null) {
                return false;
            }
            return true;
        }

        public void setRoutes(Routes routes) {
            this.routes = routes;
        }

        public String getName() {
            return this.name;
        }

        boolean hasName() {
            if (this.name == null) {
                return false;
            }
            return true;
        }

        public void setName(String name) {
            this.name = name;
        }

        public HalVersion getHalVersion() {
            return this.halVersion;
        }

        boolean hasHalVersion() {
            if (this.halVersion == null) {
                return false;
            }
            return true;
        }

        public void setHalVersion(HalVersion halVersion) {
            this.halVersion = halVersion;
        }

        static Module read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
            int type;
            Module instance = new Module();
            String raw = parser.getAttributeValue(null, "name");
            if (raw != null) {
                instance.setName(raw);
            }
            String raw2 = parser.getAttributeValue(null, "halVersion");
            if (raw2 != null) {
                HalVersion value = HalVersion.fromString(raw2);
                instance.setHalVersion(value);
            }
            parser.getDepth();
            while (true) {
                type = parser.next();
                if (type == 1 || type == 3) {
                    break;
                } else if (parser.getEventType() == 2) {
                    String tagName = parser.getName();
                    if (tagName.equals("attachedDevices")) {
                        AttachedDevices value2 = AttachedDevices.read(parser);
                        instance.setAttachedDevices(value2);
                    } else if (tagName.equals("defaultOutputDevice")) {
                        instance.setDefaultOutputDevice(XmlParser.readText(parser));
                    } else if (tagName.equals("mixPorts")) {
                        MixPorts value3 = MixPorts.read(parser);
                        instance.setMixPorts(value3);
                    } else if (tagName.equals("devicePorts")) {
                        DevicePorts value4 = DevicePorts.read(parser);
                        instance.setDevicePorts(value4);
                    } else if (tagName.equals("routes")) {
                        Routes value5 = Routes.read(parser);
                        instance.setRoutes(value5);
                    } else {
                        XmlParser.skip(parser);
                    }
                }
            }
            if (type != 3) {
                throw new DatatypeConfigurationException("Modules.Module is not closed");
            }
            return instance;
        }
    }

    public List<Module> getModule() {
        if (this.module == null) {
            this.module = new ArrayList();
        }
        return this.module;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static Modules read(XmlPullParser parser) throws XmlPullParserException, IOException, DatatypeConfigurationException {
        int type;
        Modules instance = new Modules();
        parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1 || type == 3) {
                break;
            } else if (parser.getEventType() == 2) {
                String tagName = parser.getName();
                if (tagName.equals("module")) {
                    Module value = Module.read(parser);
                    instance.getModule().add(value);
                } else {
                    XmlParser.skip(parser);
                }
            }
        }
        if (type != 3) {
            throw new DatatypeConfigurationException("Modules is not closed");
        }
        return instance;
    }
}
