'use strict';

import React from 'react';
import RmaSummary from './summary';
import CustomerInfo from './customer-info';
import LineItems from '../line-items/line-items';
import RmaShippingMethod from './shipping-method';
import RmaPayment from './payment';
import RmaStore from './store';

export default class RmaDetails extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    let rma = this.props.rma;
    let isEditing = this.state.isEditing;
    let lineColumns = [
      {field: 'imagePath', text: 'Image', type: 'image'},
      {field: 'name', text: 'Name'},
      {field: 'sku', text: 'SKU'},
      {field: 'quantity', text: 'Quantity'},
      {field: 'inventoryDisposition', text: 'Inventory Disposition'},
      {field: 'refund', text: 'Refund', type: 'currency'},
      {field: 'reason', text: 'Reason'}
    ];

    return (
      <div className="rma-details fc-grid fc-grid-match">
        <div className="fc-col-3-10">
          <RmaSummary rma={rma} isEditing={isEditing}/>
        </div>
        <div className="fc-col-7-10">
          <CustomerInfo rma={rma} isEditing={isEditing}/>
        </div>
        <div className="fc-col-1-1">
          <LineItems
            entity={rma}
            isEditing={isEditing}
            tableColumns={lineColumns}
            model={'rma'}
            />
        </div>
        <div className="fc-col-1-1">
          <RmaShippingMethod rma={rma} isEditing={isEditing}/>
        </div>
        <div className="fc-col-1-1">
          <RmaPayment rma={rma} isEditing={isEditing}/>
        </div>
      </div>
    );
  }
}

RmaDetails.propTypes = {
  rma: React.PropTypes.object
};
