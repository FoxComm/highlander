'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import Header from '../header/header';
import Menu from '../menu/menu';

export default class Site extends React.Component {
  render() {
    return (
      <div>
        <Menu/>
        <Header/>
        <main role='main'>
          <RouteHandler/>
        </main>
      </div>
    );
  }
}
