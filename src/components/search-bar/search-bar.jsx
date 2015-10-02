'use strict';

import React from 'react';

export default class SearchBar extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <div className="fc-search-bar fc-col-1-1">
        <div className="search-input-container">
          <div className="search-input-wrapper">
            <input className="fc-input-search" type="text" placeholder="Add another filter or keyword search" />
          </div>
          <div className="search-icon-wrapper">
            <i className="icon-search"></i>
          </div>
        </div>
        <div className="search-btn-container">
          <button className="fc-btn fc-btn-search">Save Search</button>
        </div>
      </div>
    );
  }
}
