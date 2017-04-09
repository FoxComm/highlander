## purposes of files here


### app.js

All middlewares and web server itself

### boot.js

configures environment for server and forks new process (from instance.js) in order to
get changed NODE_PATH actually applied for app.js

### instance.js

just imports app.js and run it
