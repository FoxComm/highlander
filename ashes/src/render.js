// import React from 'react';
// import { renderToString } from 'react-dom/server';
// import { match, RouterContext } from 'react-router';
// import { createMemoryHistory, useQueries } from 'history';
// import useNamedRoutes from 'use-named-routes';
// import { syncHistoryWithStore } from 'react-router-redux';

// import makeRoutes from './routes';
// import { Provider } from 'react-redux';
// import configureStore from './store';

// const createServerHistory = useNamedRoutes(useQueries(createMemoryHistory));

// export function * renderReact(next) {
//   const routes = makeRoutes(this.state.token);
//   const initialState = {
//     user: {
//       current: this.state.token,
//     },
//   };
//   let history = createServerHistory({
//     entries: this.url,
//     routes,
//   });

//   const store = configureStore(history, initialState);
//   history = syncHistoryWithStore(history, store);

//   let [redirectLocation, renderProps] = yield match.bind(null, { routes, location: this.url, history });

//   if (redirectLocation) {
//     this.redirect(redirectLocation.pathname + redirectLocation.search);
//   } else if (!renderProps) {
//     this.status = 404;
//   } else {
//     this.state.html = renderToString(
//       <Provider store={store} key="provider">
//         <RouterContext {...renderProps} />
//       </Provider>
//     );

//     yield next;
//   }
// }
