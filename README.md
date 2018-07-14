# uConfig Android Client

Light-weight and usable data editor and action invoker. Micro Config client is designed to provide a
simple JSON based interface for:

* Discovering uConfig servers over UDP port 8003.
* Reading and writing variables on uConfig servers.
* Invoking actions on uConfig servers.

It is considered light-weight to reduce the burden on the uConfig server, and usable because it uses
 JSON HTTP requests over a binary protocol.

Currently the only server that supports this is implemented for an ESP8266,
see [here](https://github.com/kazkansouh/lerd86/tree/master/display/).

## Building

Use Android Studio.

## uConfig Protocol Overview

Devices that support being configured by uConfig regularly broadcast beacons over the local network
on udp port 8003. These beacons are packets containing a small JSON payload. The following
is an example of a payload:

```json
{
  "beacon" : {
    "api": "/uconf/",
    "name": "Some human readable name",
    "id": "AB:34:CD"
  },
  "data" : {
    "name": "variable name",
    "value": 45,
    "type": "UINT8"
  }
}
```

Here, the `beacon` object is used for discovery and the `data` object is an optional component
that provides an update to a variable if its changed on the device. It is the choice of the server
which variables are broadcast when the value changes, and no action is performed by the client
apart from updating the UI (if applicable). The three fields of the `beacon`
have the following meaning:

* `api` is the HTTP location of the uConfig server. I.e., if the udp packet was received from
`192.168.3.67`, then the API of the uConfig would be based from `http://192.168.3.67/uconf/`.
* `name` is a name of the device. E.g., this will usually be based on the firmware and is displayed
in the uConfig client application to help differentiate types of devices.
* `id` is a unique identification for the device that is not part of the firmware. E.g. this could
be derived from the hardware mac address. The `id` is used to differentiate when multiple devices
of the same type are on the network.

The uConfig client application will expect beacons to be received regularly. If more than 45
seconds elapses between beacons, it is assumed the device has left the network and is removed from
the clients UI.

### Schema
When the client first sees a new devices beacon, it will request a schema from the device. This
defines all the variables that can be read (or written) and all actions that can be invoked. It is
expected that the schema is at the location `api + "schema"`, i.e in the above example it would be

```
http://192.168.3.67/uconf/schema
```

The client needs to perform a standard HTTP GET to access this schema. The HTTP response code should
be 200 and the content type `application/json`. The schema JSON contains two objects, `DATA` and
`ACTION`. I.e.

```json
{
  "DATA": {
    "var1" : {"READ" : true, "WRITE" : true, "TYPE" : "UINT8"},
    "var2" : {"READ" : true, "WRITE" : false, "TYPE" : "STRING"}},
  "ACTION" : {
    "action1" : {
      "param1" : "INT",
      "param2" : "STRING",
      "param3" : "INT"
    }
  }
}
```

Each member of the `DATA` object is a mapping between a variable name and a specification of that
variable. The specification defines whether the variable is readable (`true` or `false`), writable
(`true` or `false`) and the type of the variable, where the current supported types are `UINT8`,
`INT` and `STRING`.

Each member of the `ACTION` object is a mapping between a variable name and a list of the
parameters it takes (defined as parameter names and their types).

To demonstrate this, the specification of schema that allows for login credentials to be issued
to the uConfig server for a third party entity might be as follows:

```json
{
  "DATA": {
    "username" : {
      "READ" : true,
      "WRITE" : false,
      "TYPE" : "STRING"
    },
    "pin" : {
      "READ" : false,
      "WRITE" : true,
      "TYPE" : "INT"
    }
  },
  "ACTION": {
    "newuser" : {
      "user": "STRING",
      "pin":"INT"
    }
  }
}
```

Here, two variables are defined, one for the `username` and one for the integral `pin`. Access
control on these variables allows for the `username` to be read and the `pin` to be changed, as
if the `username` could be changed without clearing the password it presents a security weakness.
Therefore, to register a username, the action `newuser` should be invoked that will set both
the `username` and new `pin`.

### Read Variable

To read a variable, a HTTP GET is issued to the location identified by the API concatenated with
`get` and the query parameter `var` set to the variable name (that has been URL encoded). Consider
the running example, to read the `username` the following would be requested from the uConfig
server:

```
http://192.168.3.67/uconf/get?var=username
```

If successful. the HTTP response code should be 200 and the content type `application/json`. The
response should be a JSON object that has the variable name mapped to the value. In the
above request, the response would be of the form:

```json
{"username": "joebloggs"}
```

If the variable is name is not found, then a HTTP response of 404 is emitted.

#### Data Type Mapping

The currently supported data types are mapped to and from JSON objects as follows:

* `UINT8` and `INT` injectivly map to JSON numbers, i.e. numbers specified without `"`.
* `STRING` maps a JSON string.

### Write Variable

To write a variable, a HTTP GET is issued to the location identified by the API concatenated with
`set` and two query parameters: `var` and `val`. `var` defines the variable that should be set,
and `val` defines the value it should be set to. Consider the running example, to write the
`pin` the following would be requested from the uConfig server:

```
http://192.168.3.67/uconf/set?var=pin&val=12345
```

If successful. the HTTP response code should be 200 and the content type `application/json`. The
response should be a JSON object of the form:

```json
{"result": "ok"}
```

If not successful, the response code would be 400 or 404. The 404 code covers the case where the
variable was not found. If the code is 404, then the content type should be set to
`application/json` and the JSON object includes a with a textual description of the form:

```json
{"result": "error message here"}
```

If the server chooses to broadcast the change in value, a UDP beacon will be emitted with the
variable name and value included in the `DATA` object.

#### Data Type Mapping

All data types are mapped to canonical string representations, then URL encoded. That is,
`UINT8` and `INT` are mapped to their base 10 representation and `STRING` is mapped to a string
that does not include the `"`.

### Action Invoke

To invoke an action, a HTTP GET is issued to the location identified by the API concatenated with
`invoke`, the query parameter `method` and any additional parameters the action requires (as
defined in the schema). The `method` parameter should be set to the name of the action to execute.

The encoding of the parameters follows the same data type mapping used for the write variable
functionality. The result of requesting the action is invoked follows the same format as the
write variable functionality. However, in most cases the server will choose to invoke the action
asynchronously so the `{"result": "ok"}` result means that the server will start execution
of the action.

Consider the running example, set the `username` and `pin` the `newuser` action should be invoked
by requesting the following from the the uConfig server:

```
http://192.168.3.67/uconf/invoke?method=newuser&user=joe%40bloggs&pin=443
```

This invokes the action, and sets the user parameter to "joe@bloggs" (notice that the string has
been URL encoded) with the pin "443".
