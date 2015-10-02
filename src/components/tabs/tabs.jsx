'use strict';

import React, { PropTypes } from 'react';

export default class TabListView extends React.Component {

  static propTypes = {
    children: PropTypes.array
  };

  render() {
    return (
      <ul className="fc-tab-list">
        {React.Children.map(this.props.children, (child, idx) => {
          return React.cloneElement(child, {
              key: `tab-${idx}`
          });
        })}
      </ul>
    );
  }
}
