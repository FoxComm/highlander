'use strict';

import React from 'react';

const renderItem = child => <li>{child}</li>;

const LocalNav = props => {
  return (
    <div className="fc-grid">
      <div className="fc-col-md-1-1">
        <ul className="fc-tabbed-nav">
          {React.Children.map(props.children, renderItem)}
        </ul>
      </div>
    </div>
  );
};

export default LocalNav;
