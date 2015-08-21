'use strict';

import React from 'react';
import OrderSummary from './summary';
import OrderLineItems from './line-items';
import OrderShippingAddress from './shipping-address';
import OrderShippingMethod from './shipping-method';
import OrderPayment from './payment';
import OrderStore from './store';
import Api from '../../lib/api';
import { dispatch } from '../../lib/dispatcher';

export default class OrderDetails extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isEditing: false
    };
  }

  toggleEdit() {
    Api.post(`${OrderStore.uri(this.props.order.id)}/edit`)
      .then(() => {
        this.setState({
          isEditing: !this.state.isEditing
        });
        dispatch('toggleOrderEdit');
      })
      .catch((err) => { console.log(err); });
  }

  render() {
    let
      order     = this.props.order,
      isEditing = this.state.isEditing,
      actions   = null;

    if (isEditing) {
      actions = (
        <span>
          <button onClick={this.toggleEdit.bind(this)}>Cancel</button>
          <button>Save Edits</button>
        </span>
      );
    } else if (OrderStore.holdStatusList.indexOf(order.orderStatus) !== -1) {
      actions = <button className='btn' onClick={this.toggleEdit.bind(this)}>Edit Order Details</button>;
    }

    return (
      <div id="order-details">
        {actions}
        <OrderSummary order={order} isEditing={isEditing}/>
        <article>
          <OrderLineItems order={order} isEditing={isEditing}/>
          <OrderShippingAddress order={order} isEditing={isEditing}/>
          <OrderShippingMethod order={order} isEditing={isEditing} />
          <OrderPayment order={order} isEditing={isEditing}/>
        </article>
      </div>
    );
  }
}

OrderDetails.propTypes = {
  order: React.PropTypes.object
};
