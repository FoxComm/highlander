'use strict';

import React from 'react';

export default class TabView extends React.Component {
  constructor(props) {

  }

  render() {
    return (
      <li className="fc-tab">
        <i className="fa fa-bars"></i>&nbsp;
        {this.props.children}
      </li>
    );
  }
}

TabView.propTypes = {
  children: React.PropTypes.array
};
