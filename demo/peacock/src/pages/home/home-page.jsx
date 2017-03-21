/* @flow */

// libs
import React, { Component } from 'react';
import { browserHistory } from 'lib/history';

class HomePage extends Component {
  componentDidMount() {
    browserHistory.push('/ALL');
  }

  render() {
    return null;
  }
};

export default HomePage;
