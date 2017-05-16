import _ from 'lodash';
import React from 'react';
import { render } from 'react-dom';

const DEBUG = process.env.NODE_ENV != 'production';

function renderApp() {
//  const history = browserHistory;

  render((
    <div>Nexus</div>
  ), document.getElementById('app'));
}


renderApp();
