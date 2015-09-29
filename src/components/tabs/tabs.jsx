'use strict';

import React from 'react';

export default class TabListView extends React.Component {
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

TabListView.propTypes = {
  children: React.PropTypes.array
}
