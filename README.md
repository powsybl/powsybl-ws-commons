# PowSyBl Web services commons

[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
[![Join the community on Spectrum](https://withspectrum.github.io/badge/badge.svg)](https://spectrum.chat/powsybl)
[![Slack](https://img.shields.io/badge/slack-powsybl-blueviolet.svg?logo=slack)](https://join.slack.com/t/powsybl/shared_invite/zt-rzvbuzjk-nxi0boim1RKPS5PjieI0rA)


## TODO
```
LogUtils
SecuredZipInputStream
```


## Spring-Boot auto-configure module
A module using Spring-Boot `@AutoConfigure` mechanism is provided
by this library to configure spring-boot modules.  
This spring-boot module is configurable by the properties domain `powsybl-ws.autoconfigure.*`.

### Skip whole module initialization
To skip all of the module initialization and configuration, you can
exclude it: `@SpringBootApplication(exclude={PowsyblWsCommonAutoConfiguration.class})`,
alias for `@EnableAutoConfiguration(exclude={PowsyblWsCommonAutoConfiguration.class})`,
or use the `spring.autoconfigure.exclude` property.

### Tomcat configuration
The following properties are available under `powsybl.autoconfigure.tomcat-customize.*`:

| Property                 | type    | default | Description                                                                                                                                                                                                                                                                          |
|--------------------------|---------|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| enable                   | boolean | true    | Enable Tomcat Connector customization                                                                                                                                                                                                                                                |
| encoded-solidus-handling | boolean | true    | Set Tomcat Connector [`encodedSolidusHandling` attribute](https://tomcat.apache.org/tomcat-10.1-doc/config/http.html#Common_Attributes) to [`PASS_THROUGH` value](https://tomcat.apache.org/tomcat-10.1-doc/api/org/apache/tomcat/util/buf/EncodedSolidusHandling.html#PASS_THROUGH) |
