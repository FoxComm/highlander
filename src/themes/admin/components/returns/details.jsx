'use strict';

import React from 'react';
import ReturnSummary from './summary';
import CustomerInfo from './customer-info';
import ReturnLineItems from './line-items';
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
    let actions = null;

    return (
      <div className="return-details">
        <div className="return-details-controls">
          {actions}
        </div>
        <div className="return-details-body fc-grid fc-grid-match">
          <div className="fc-col-3-10">
            <ReturnSummary return={retrn} isEditing={isEditing}/>
          </div>
          <div className="fc-col-7-10">
            <CustomerInfo return={retrn} isEditing={isEditing}/>
          </div>
          <div className="return-details-main">
            <ReturnLineItems return={retrn} isEditing={isEditing}/>
            <ReturnShippingMethod return={retrn} isEditing={isEditing} />
            <ReturnPayment return={retrn} isEditing={isEditing}/>
          </div>
        </div>
      </div>
    );
  }
}

ReturnDetails.propTypes = {
  return: React.PropTypes.object
};
