'use strict';

import React, { PropTypes } from 'react';

export default class TabView extends React.Component {

  static propTypes = {
    children: PropTypes.node
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      selected: false
    };
  }

  render() {
    return (
      <li className="fc-tab">
        <i className="icon-drag-drop"></i>&nbsp;
        {this.props.children}
      </li>
    );
  }
}
