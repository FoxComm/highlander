
import _ from 'lodash';
import React, { Component } from 'react';
import styles from './checkout.css';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

import Button from 'ui/buttons';
import Checkbox from 'ui/checkbox';
import EditableBlock from 'ui/editable-block';
import { FormField, Form } from 'ui/forms';
import Currency from 'ui/currency';

import * as checkoutActions from 'modules/checkout';

const ViewDelivery = props => {
  return (
    <div>view delivery</div>
  );
};

class EditDelivery extends Component {

  @autobind
  handleSubmit() {

  }

  render() {
    return (
      <Form onSubmit={this.handleSubmit}>
        <div styleName="shipping-method">
          <Checkbox name="delivery" id="d1">7-10 DAY STANDARD</Checkbox>
          <div styleName="delivery-cost">FREE</div>
        </div>
        <div styleName="shipping-method">
          <Checkbox name="delivery" id="d2">2 DAY AIR</Checkbox>
          <Currency styleName="delivery-cost" value="1000"/>
        </div>
        <div styleName="shipping-method">
          <Checkbox name="delivery" id="d3">Overnight</Checkbox>
          <Currency styleName="delivery-cost" value="4000"/>
        </div>
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
