'use strict';

import React from 'react';

export default class SearchBar extends React.Component {

  render() {
    return (
      <div className="fc-col-md-1-1 fc-search-bar">
        <form>
          <div className="fc-search-input-container">
            <div className="fc-search-input-wrapper fc-form-field">
              <input className="fc-input-search" type="text" placeholder="Add another filter or keyword search" />
            </div>
            <div className="fc-search-icon-wrapper">
              <i className="icon-search"></i>
            </div>
          </div>
          <div className="fc-search-btn-container">
            <button className="fc-btn fc-btn-search">Save Search</button>
          </div>
        </form>
      </div>
    );
  }
}
