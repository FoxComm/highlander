'use strict';

import React from 'react';
import OrderSummary from './summary';
import OrderLineItems from './line-items';

export default class OrderDetails extends React.Component {
  render() {
    let order = this.props.order;

    return (
      <div id="order-details">
        <OrderSummary order={order}/>
        <article>
          <OrderLineItems order={order}/>
        </article>
      </div>
    );
  }
}

OrderDetails.propTypes = {
  order: React.PropTypes.object
};
