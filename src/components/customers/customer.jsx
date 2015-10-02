'use strict';

import React from 'react';

export default class Customer extends React.Component {
  render() {
    return (
      <div id="user">
        {this.props.children}
      </div>
    );
  }
}
