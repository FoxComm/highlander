import React from 'react';
import { renderToString } from 'react-dom/server';
import { match, RouterContext } from 'react-router';
import createHistory from 'history/lib/createMemoryHistory';
import { useQueries } from 'history';
import { Provider } from 'react-redux';

import makeStore from './store';
import makeRoutes from './routes';
import I18nProvider from 'lib/i18n/provider';
import renderPage from '../build/main.html';

const createServerHistory = useQueries(createHistory);

function getAssetsNames() {
  let appJs = 'app.js';
  let appCss = 'app.css';

  if (process.env.NODE_ENV === 'production') {
    try {
      const revManifest = require('../build/rev-manifest.json');

      appJs = revManifest['app.js'];
      appCss = revManifest['app.css'];
    } catch (e) {
      console.error('rev-manifest.json not found');
    }
  }

  return { appJs, appCss };
}

const assetsNames = getAssetsNames();

export default function *renderReact() {
  const history = createServerHistory(this.url);

  const authHeader = this.get('Authorization');

  const { auth, i18n } = this.state;
  const initialState = auth ? {auth} : {};
  if (authHeader) initialState.authHeader = authHeader;

  const store = makeStore(history, initialState);
  const routes = makeRoutes(store);

  const [redirectLocation, renderProps] = yield match.bind(null, { routes, location: this.url, history });

  if (redirectLocation) {
    this.redirect(redirectLocation.pathname + redirectLocation.search);
  } else if (!renderProps) {
    this.status = 404;
  } else {
    const rootElement = (
      <I18nProvider locale={i18n.language} translation={i18n.translation}>
        <Provider store={store} key="provider">
          <RouterContext {...renderProps} />
        </Provider>
      </I18nProvider>
    );

    const appHtml = yield store.renderToString(renderToString, rootElement);
    const { appJs, appCss } = assetsNames;

    this.body = renderPage({
      html: appHtml,
      state: JSON.stringify(store.getState()),
      i18n: JSON.stringify(i18n),
      stripeApiKey: JSON.stringify(process.env.STRIPE_PUBLISHABLE_KEY || null),
      appJs,
      appCss,
    });
  }
}
