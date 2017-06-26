import React from 'react';
import PropTypes from 'prop-types';
import { CustomerInfo } from './helpers';
// import TotalsSummary from '../common/totals';
import Payment from '../payment/payment';

export default class RmaDetails extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      isEditing: false
    };
  }

  static propTypes = {
    entity: PropTypes.object
  };

  get lineItemColumns() {
    return [
      {field: 'imagePath', text: 'Image', type: 'image'},
      {field: 'name', text: 'Name'},
      {field: 'sku', text: 'SKU'},
      {field: 'quantity', text: 'Quantity'},
      {field: 'inventoryDisposition', text: 'Inventory Disposition'},
      {field: 'refund', text: 'Refund', type: 'currency'},
      {field: 'reason', text: 'Reason'}
    ];
  }

  render() {
    let rma = this.props.entity;
    let isEditing = this.state.isEditing;

    return (
      <div className="fc-rma-details fc-grid fc-grid-match">
        <div className="fc-col-md-3-10">
          {/*<TotalsSummary entity={rma} title="Return" />*/}
        </div>
        <div className="fc-col-md-7-10">
          <CustomerInfo rma={rma} isEditing={isEditing} />
        </div>
        <div className="fc-col-md-1-1">
          {/*<LineItems
             entity={rma}
             isEditing={isEditing}
             tableColumns={this.lineItemColumns}
             model={'rma'}
             />*/}
        </div>
        <div className="fc-col-md-1-1">
        </div>
        <div className="fc-col-md-1-1">
          <Payment rma={rma} isEditing={isEditing} />
        </div>
      </div>
    );
  }
}
