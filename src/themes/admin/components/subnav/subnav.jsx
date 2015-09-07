'use strict';

import React from 'react';

export default class SubNav extends React.Component {
  render() {
    return (
      <nav className="fc-subnav">
        <ul>
          {React.Children.map(this.props.children, (child, idx) => {
            return React.cloneElement(child, {
              key: `${idx}-subnav`
            });
          })}
        </ul>
      </nav>
    );
  }
}

SubNav.propTypes = {
  children: React.PropTypes.array
};
