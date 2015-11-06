'use strict';

import React, { PropTypes } from 'react';

const renderItem = child => {
  if (child.type === 'li') {
    return React.cloneElement(child, {
      className: 'fc-tabbed-nav-parent'
    });
  } else {
    return <li>{child}</li>;
  }
};

const LocalNav = props => {
  return (
    <div className={`fc-grid ${props.gutter ? 'fc-grid-gutter' : ''}`}>
      <div className="fc-col-md-1-1">
        <ul className="fc-tabbed-nav">
          {React.Children.map(props.children, renderItem)}
        </ul>
      </div>
    </div>
  );
};

LocalNav.propTypes = {
  gutter: PropTypes.bool
};

LocalNav.defaultProps = {
  gutter: false
};

export default LocalNav;
