/* @flow */

import React, { Component } from 'react';
import { Router } from 'react-router';
import { Provider } from 'react-redux';

export default class Root extends Component {
  render() {
    return (
      <Provider store={this.props.store} routes={this.props.routes} key="provider">
        <Router history={this.props.history}>
          {this.props.routes}
        </Router>
      </Provider>
    );
  }
}
