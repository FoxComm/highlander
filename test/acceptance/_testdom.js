
const jsdom = require('jsdom').jsdom;
const markup = '<html><body></body></html>';

function init() {
  if (typeof document !== 'undefined') return;
  global.document = jsdom(markup, {
    features: {
      FetchExternalResources: ['script', 'link', 'img']
    },
    resourceLoader: function(resource, callback) {
      const pathname = resource.url.pathname;
      console.log('GET======>', pathname);

      if (/\.svg$/.test(pathname)) {
        setTimeout(function() {
          resource.defaultFetch(callback);
        }, 200);
      } else {
        return resource.defaultFetch(callback);
      }

    }
  });
  global.window = document.defaultView;
  global.navigator = window.navigator;
  global.HTMLElement = window.HTMLElement;
  console.log('jsdom configured');
}

init();


