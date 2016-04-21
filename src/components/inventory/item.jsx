
//libs
import React, { PropTypes } from 'react';
import { haveType } from '../../modules/state-helpers';

// components
import { Link, IndexLink } from '../link';
import LocalNav, { NavDropdown } from '../local-nav/local-nav';
import { PageTitle } from '../section-title';

const InventoryItem = props => {

  const content = React.cloneElement(props.children, {entity: haveType(props.params, 'inventory-item') });

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
          <Link to="inventory-item-details" params={props.params}>Inventory</Link>
          <Link to="inventory-item-notes" params={props.params}>Notes</Link>
          <Link to="inventory-item-activity-trail" params={props.params}>Activity Trail</Link>
        </LocalNav>
      </div>
      {content}
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
