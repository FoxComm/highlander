
import React from 'react';
import { Router, browserHistory } from 'react-router';
import { ReduxAsyncConnect } from 'redux-async-connect';
import { Provider } from 'react-redux'
import { render } from 'react-dom'
import makeStore from './store';
import routes from './routes';

export function renderApp() {
  const store = makeStore(browserHistory, window.__data);

  render((
    <Provider store={store} key="provider">
      <Router render={(props) => <ReduxAsyncConnect {...props}/>} history={browserHistory}>
        {routes}
      </Router>
    </Provider>
  ), document.getElementById('app'));
}



