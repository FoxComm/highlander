'use strict';

import React from 'react';
import {render} from 'react-dom';
import { renderToString } from 'react-dom/server';
import Router,  { RoutingContext, match } from 'react-router';
import createBrowserHistory from 'history/lib/createBrowserHistory';
import createLocation from 'history/lib/createLocation';
import routes from './routes';

const app = {

  start() {
    let history = createBrowserHistory();
    render(<Router history={history}>{routes}</Router>, document.getElementById('foxcom'));
  },

  * renderReact(next) {
    let location = createLocation(this.path);

    let [redirectLocation, renderProps] = yield match.bind(null, {routes, location});

    if (redirectLocation) {
      this.redirect(redirectLocation.pathname + redirectLocation.search);
    } else if (renderProps == null) {
      this.status = 404;
    } else {
      this.state.html = renderToString(<RoutingContext {...renderProps}/>);
      yield next;
    }
  }
};

export default app;
