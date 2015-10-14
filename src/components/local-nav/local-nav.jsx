'use strict';

import React from 'react';

export default class LocalNav extends React.Component {
  static propTypes = {
    children: React.PropTypes.array
  }

  renderItem(child) {
    return (
      <li>{React.cloneElement(child)}</li>
    );
  }

  render() {
    return (
      <div className="fc-grid">
        <div className="fc-col-md-1-1">
          <ul className="fc-tabbed-nav">
            {React.Children.map(this.props.children, this.renderItem)}
          </ul>
        </div>
      </div>
    );
  }
}
