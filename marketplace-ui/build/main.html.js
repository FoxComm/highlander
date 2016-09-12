module.exports = function anonymous(it
/**/) {
var out='<!doctype html><html> <head> <title>StoreFront</title> <meta name="viewport" content="width=device-width"> <link rel="stylesheet" href="/app.css" /> </head> <body> <div id="app">'+( it.html )+'</div> <script charset="UTF-8"> window.initialState='+( it.state )+'; </script> <script type="text/javascript" src="/app.js"></script> <script type="text/javascript"> App.renderApp(); </script> </body></html>';return out;
}