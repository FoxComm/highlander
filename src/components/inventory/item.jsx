
//libs
import React, { PropTypes } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav, { NavDropdown } from '../local-nav/local-nav';
import { PageTitle } from '../section-title';

export default class InventoryItem extends React.Component {
  render() {
    return (
      <div className="fc-inventory-item">
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <PageTitle title="SKU" subtitle={this.props.params.sku} />
          </div>
        </div>
        <LocalNav gutter={true}>
          <a href="">General</a>
          <IndexLink to="inventory-item-details" params={this.props.params}>Inventory</IndexLink>
          <a href="">Notes</a>
          <a href="">Activity Trail</a>
        </LocalNav>
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            {this.props.children}
          </div>
        </div>
      </div>
    );
  }
}
