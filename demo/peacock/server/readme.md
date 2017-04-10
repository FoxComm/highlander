## purposes of files here

### app.js

All middlewares and web server itself

### setup_env.js
configures environment for the server

### boot.js

executes setup_env and forks new server process (from app.js) in order to
get changed NODE_PATH actually applied for app.js

