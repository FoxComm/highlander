#service

HTTP service implementation.

| METHOD | REQUEST                       | PAYLOAD | RESP                                                                                               |
|:---------------------------------------|:-------------------------------------------------------------------------------------------------------------|
| GET | /ping                            |  | pong |
| GET | /find                            |  {"scope": <num>, "group" : <string> , traits: { "trait1": <num or string>, "trait2": ...}| {"ref": <cluster ref>, "dist": <num>, "traits" : <object>} |
