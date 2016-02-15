import React from 'react';
import { render } from 'react-dom';
import { renderToString } from 'react-dom/server';
import Login from './components/auth/login';

const app = {

  start() {

    render(
      <Login/>,
      document.getElementById('foxcom')
    );
  },

  * renderReact(next) {
    this.state.html = renderToString(
      <Login/>
    );

    yield next;
  }
};

export default app;
