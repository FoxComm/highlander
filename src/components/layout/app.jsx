
/* eslint-disable no-unused-vars */

import reset from '../../css/reset.css';

/* eslint-enable no-unused-vars */

import React, { PropTypes } from 'react';
import styles from './app.css';
import cssModules from 'react-css-modules';

@cssModules(styles)
export default class App extends React.Component {

  static propTypes = {
    children: PropTypes.node,
  };

  render() {
    return this.props.children;
  }
}

export default App;
