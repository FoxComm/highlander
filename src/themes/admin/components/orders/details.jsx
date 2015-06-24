'use strict';

import React from 'react';
import OrderSummary from './summary';

export default class OrderDetails extends React.Component {
  render() {
    let order = this.props.order;

    return (
      <div id="order-details">
        <OrderSummary order={order}/>
      </div>
    );
  }
}

OrderDetails.propTypes = {
  order: React.PropTypes.object
};
