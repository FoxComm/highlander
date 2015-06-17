'use strict';

import React from 'react';

export default class Header extends React.Component {
  render() {
    return (
      <header role='banner'>
        <div className="breadcrumb">Orders</div>
        <div className="sub-nav">
          <div className="notifications">
            <i className="icon-bell-alt"></i>
          </div>
          <div className="sort">Name <i className="icon-down-open"></i></div>
        </div>
      </header>
    );
  }
}
