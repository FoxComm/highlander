
import React, { Component } from 'react';
import styles from './checkout.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import Button from 'ui/buttons';
import Checkbox from 'ui/checkbox';
import EditableBlock from 'ui/editable-block';
import { Form } from 'ui/forms';
import Currency from 'ui/currency';

import * as checkoutActions from 'modules/checkout';

const ViewDelivery = () => {
  return (
    <div>view delivery</div>
  );
};

/* ;;`*/
@connect(state => state.checkout, checkoutActions)
/* ;;`*/
class EditDelivery extends Component {

  componentWillMount() {
    this.props.fetchShippingMethods();
  }

  @autobind
  handleSubmit() {

  }

  get shippingMethods() {
    const { shippingMethods } = this.props;

    return shippingMethods.map(shippingMethod => {
      const cost = shippingMethod.price == 0
        ? <div styleName="delivery-cost">FREE</div>
        : <Currency styleName="delivery-cost" value={shippingMethod.price}/>;

      return (
        <div key={shippingMethod.id} styleName="shipping-method">
          <Checkbox name="delivery" id={shippingMethod.id}>{shippingMethod.name}</Checkbox>
          {cost}
        </div>
      );
    });
  }

  render() {
    return (
      <Form onSubmit={this.handleSubmit}>
        {this.shippingMethods}
        <Button styleName="checkout-submit" type="submit">CONTINUE</Button>
      </Form>
    );
  }
}

const Delivery = props => {
  const deliveryContent = (
    <div styleName="checkout-block-content">
      {props.isEditing ? <EditDelivery {...props} /> : <ViewDelivery />}
    </div>
  );

  return (
    <EditableBlock
      styleName="checkout-block"
      title="DELIVERY"
      isEditing={props.isEditing}
      collapsed={props.collapsed}
      editAction={props.editAction}
      content={deliveryContent}
    />
  );
};

export default Delivery;
