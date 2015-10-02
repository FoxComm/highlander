'use strict';

import React from 'react';

export default class TabView extends React.Component {
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

TabView.propTypes = {
  children: React.PropTypes.array
};
