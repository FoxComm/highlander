'use strict';

import React, { PropTypes } from 'react';
import Link from './link.jsx';

export default class IndexLink extends React.Component {

  static propTypes = {
    children: PropTypes.node
  };

  render() {
    return <Link {...this.props} onlyActiveOnIndex={true}>{this.props.children}</Link>;
  }
}
