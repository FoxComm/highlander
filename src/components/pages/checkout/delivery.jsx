
import _ from 'lodash';
import React, { Component } from 'react';
import cssModules from 'react-css-modules';
import styles from './checkout.css';
import { autobind, debounce } from 'core-decorators';
import { connect } from 'react-redux';

import Button from 'ui/buttons';
import { TextInput } from 'ui/inputs';
import EditableBlock from 'ui/editable-block';
import { FormField, Form } from 'ui/forms';
import Autocomplete from 'ui/autocomplete';

import * as checkoutActions from 'modules/checkout';

const ViewDelivery = props => {
  return (
    <div>view delivery</div>
  );
};

/* ::`*/
@cssModules(styles)
/* ::`*/
class EditDelivery extends Component {

  @autobind
  handleSubmit() {

  }

  render() {
    return (
      <Form onSubmit={this.handleSubmit}>
        <Button styleName="checkout-submit" type="submit">CONTINUE</Button>
      </Form>
    );
  }
}

const Delivery = props => {
  return (
    <EditableBlock
      styleName="checkout-block"
      title="DELIVERY"
      isEditing={props.isEditing}
      collapsed={props.collapsed}
      editAction={props.editAction}
      viewContent={<ViewDelivery />}
      editContent={<EditDelivery {...props} />}
    />
  );
};

export default cssModules(Delivery, styles);
