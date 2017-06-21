const jsdom = require('jsdom').jsdom;
const markup = '<html><body></body></html>';
const fs = require('fs');
const qs = require('query-string');

function init() {
  if (typeof document !== 'undefined') return;
  global.document = jsdom(markup, {
    features: {
      FetchExternalResources: ['script', 'link', 'img'],
    },
    resourceLoader: function(resource, callback) {
      const pathname = resource.url.pathname;
      if (pathname.startsWith('/resources')) {
        const query = qs.parse(resource.url.search);
        const reply = function() {
          return callback(null, fs.readFileSync(`${__dirname}/_${pathname.substr(1)}`));
        };

        if (query.timeout) {
          setTimeout(reply, Number(query.timeout));
        } else {
          return reply();
        }
      } else {
        return resource.defaultFetch(callback);
      }
    },
  });
  global.window = document.defaultView;
  global.Image = global.window.Image;
  global.navigator = window.navigator;
  global.HTMLElement = window.HTMLElement;
  console.info('jsdom configured');
}

init();
