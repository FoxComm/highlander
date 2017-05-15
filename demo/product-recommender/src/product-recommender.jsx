import React from 'react';
import { createAppHistory } from 'lib/history';

import { Provider } from 'react-redux';
import { render } from 'react-dom';
import makeStore from './store';
import makeRoutes from './routes';
import I18nProvider from 'lib/i18n/provider';

import RelatedProducts from './pages/catalog/related-products';

export default function renderProductRecommender(productID, elementID = 'product-recommender') {
  const routes = makeRoutes();
  const history = createAppHistory({
    routes,
  });
  const store = makeStore(history, window.__data);

  const {language, translation} = window.__i18n;

  render((
    <I18nProvider locale={language} translation={translation}>
      <Provider store={store} key="provider">
        <RelatedProducts id={productID} />
      </Provider>
    </I18nProvider>
  ), document.getElementById(elementID));
}
