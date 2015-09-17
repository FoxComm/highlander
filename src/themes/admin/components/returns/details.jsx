'use strict';

import React from 'react';
import ReturnSummary from './summary';
import CustomerInfo from './customer-info';
import LineItems from '../line-items/line-items';
import ReturnShippingMethod from './shipping-method';
import ReturnPayment from './payment';
import ReturnStore from './store';

export default class ReturnDetails extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    let retrn = this.props.return;
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
      <div className="return-details fc-grid fc-grid-match">
        <div className="fc-col-3-10">
          <ReturnSummary return={retrn} isEditing={isEditing}/>
        </div>
        <div className="fc-col-7-10">
          <CustomerInfo return={retrn} isEditing={isEditing}/>
        </div>
        <div className="fc-col-1-1">
          <LineItems
            entity={retrn}
            isEditing={isEditing}
            tableColumns={lineColumns}
            model={'return'}
            />
        </div>
        <div className="fc-col-1-1">
          <ReturnShippingMethod return={retrn} isEditing={isEditing}/>
        </div>
        <div className="fc-col-1-1">
          <ReturnPayment return={retrn} isEditing={isEditing}/>
        </div>
      </div>
    );
  }
}

ReturnDetails.propTypes = {
  return: React.PropTypes.object
};
