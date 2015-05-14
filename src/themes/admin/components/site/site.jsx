'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';
import Header from '../header/header';

class Site extends React.Component {
  render() {
    return (
      <div>
        <Header/>
        <main role='main'>
          <RouteHandler/>
        </main>
      </div>
    );
  }
}

export default Site;
