'use strict';

import React from 'react';

export default class TabView extends React.Component {
  render() {
    return (
      <ul>
        {React.Children.map(this.props.children, (child, idx) => {
          return React.cloneElement(child, {
              key: `tab-${idx}`
          });
        })}
      </ul>
    );
  }
}

TabView.propTypes = {
  children: React.PropTypes.array
}
