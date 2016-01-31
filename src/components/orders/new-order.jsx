import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';

import * as newOrderActions from '../../modules/orders/new-order';

import { Button } from '../common/buttons';
import BigCheckbox from '../checkbox/big-checkbox';
import ChooseCustomer from './choose-customer';
import ChooseCustomerRow from './choose-customer-row';
import Form from '../forms/form';
import FormField from '../forms/formfield';
import Typeahead from '../typeahead/typeahead';

function mapStateToProps(state) {
  return {
    newOrder: state.orders.newOrder,
  };
}

@connect(mapStateToProps, { ...newOrderActions})
export default class NewOrder extends Component {
  static propTypes = {
    suggestCustomers: PropTypes.func.isRequired,
  };

  get suggestedCustomers() {
    const empty = { rows: [], from: 0, size: 0, total: 0 };
    return _.get(this.props, 'newOrder.result', empty);
  }

  get isFetching() {
    return _.get(this.props, 'newOrder.isFetching', false);
  }

  get customersList() {
    return (
      <ChooseCustomer items={this.suggestedCustomers} />
    );
  }

  render() {
    return (
      <div className="fc-order-create">
        <div className="fc-grid">
          <header className="fc-order-create__header fc-col-md-1-1">
            <h1 className="fc-title">
              New Order
            </h1>
          </header>
          <article className="fc-col-md-1-1">
            <div className="fc-grid fc-order-create__customer-form-panel">
              <div className="fc-order-create__customer-form-subtitle fc-col-md-1-1">
                <h2>Customer</h2>
              </div>
              <div className="fc-order-create__customer-form fc-col-md-1-1">
                <Form autoComplete="off" className="fc-grid fc-grid-no-gutter">
                  <Typeahead
                    className="fc-order-create__customer-search fc-col-md-5-8"
                    component={ChooseCustomerRow}
                    fetchItems={this.props.suggestCustomers}
                    isFetching={this.isFetching}
                    itemsElement={this.customersList}
                    placeholder="Customer name or email..." />
                  <FormField
                    className="fc-order-create__guest-checkout fc-col-md-2-8"
                    label="Checkout as guest"
                    labelAfterInput={true}>
                    <BigCheckbox name="guestCheckout" />
                  </FormField>
                  <FormField className="fc-col-md-1-8">
                    <Button className="fc-btn-primary fc-right">
                      Next
                      <i className="icon-chevron-right" />
                    </Button>
                  </FormField>
                </Form>
              </div>
            </div>
          </article>
        </div>
      </div>
    );
  }
};
