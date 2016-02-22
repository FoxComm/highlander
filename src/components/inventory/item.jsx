
//libs
import React, { PropTypes } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav, { NavDropdown } from '../local-nav/local-nav';
import { PageTitle } from '../section-title';

const InventoryItem = props => {
  return (
    <div className="fc-inventory-item">
      <div className="fc-inventory-item__summary">
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            <PageTitle title="SKU" subtitle={props.params.sku} />
          </div>
        </div>
        <LocalNav gutter={true}>
          <a href="">General</a>
          <IndexLink to="inventory-item-details" params={props.params}>Inventory</IndexLink>
          <a href="">Notes</a>
          <a href="">Activity Trail</a>
        </LocalNav>
      </div>
      {props.children}
    </div>
  );
};

InventoryItem.propTypes = {
  params: PropTypes.shape({
    sku: PropTypes.string,
  }),
  children: PropTypes.node,
};

export default InventoryItem;
