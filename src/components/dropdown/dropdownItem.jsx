'use strict';

import React from 'react';

const DropdownItem = (props) => {
  return (
    <div className="fc-dropdown-item" key={props.value} onClick={props.onClick}>
      {props.children}
    </div>
  );
};

DropdownItem.propTypes = {
  onClick: React.PropTypes.func,
  value: React.PropTypes.string,
  children: React.PropTypes.any
};

export default DropdownItem;