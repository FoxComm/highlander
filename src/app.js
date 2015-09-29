'use strict';

import React from 'react';
import Router from 'react-router';
import { canUseDOM } from 'react/lib/ExecutionEnvironment';
import routes from './routes';

let app = {
  start(bootstrap) {
    return canUseDOM ? this.webStart() : this.serverStart(bootstrap);
  },

  webStart() {
    Router.run(routes, Router.HistoryLocation, (Handler) => {
      React.render(<Handler/>, document.getElementById('foxcom'));
    });
  },

  serverStart(bootstrap) {
    return (fn) => {
      Router.run(routes, bootstrap.path, (Handler) => {
        let
          component = React.createFactory(Handler),
          html      = React.renderToString(component(bootstrap));
        fn(null, html);
      });
    };
  }
};

export default app;
