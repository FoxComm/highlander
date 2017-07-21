/* @flow */

// libs
import React from 'react';
import { browserHistory } from 'lib/history';

class HomePage extends React.Component {
  componentDidMount() {
    browserHistory.push('/ALL');
  }

  render() {
    return null;
  }
}

export default HomePage;
