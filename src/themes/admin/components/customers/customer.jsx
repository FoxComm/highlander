'use strict';

import React from 'react';
import { RouteHandler } from 'react-router';

export default class Customer extends React.Component {
  render() {
    return (
      <div id="user">
        <RouteHandler/>
      </div>
    );
  }
}
