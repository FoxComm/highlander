import React, { Component, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { transitionTo } from '../../route-helpers';
import _ from 'lodash';

import * as newOrderActions from '../../modules/orders/new-order';
import { email } from '../../lib/validators';

import { Button, PrimaryButton } from '../common/buttons';
import BigCheckbox from '../checkbox/big-checkbox';
import ChooseCustomer from './choose-customer';
import ChooseCustomerRow from './choose-customer-row';
import ContentBox from '../content-box/content-box';
import ErrorAlerts from '../alerts/error-alerts';
import Form from '../forms/form';
import FormField from '../forms/formfield';
import PilledInput from '../pilled-search/pilled-input';
import PageTitle from '../section-title/page-title';
import Typeahead from '../typeahead/typeahead';

function mapStateToProps(state) {
  return {
    newOrder: state.orders.newOrder,
  };
}

@connect(mapStateToProps, { ...newOrderActions})
export default class NewOrder extends Component {
  static propTypes = {
    createOrder: PropTypes.func.isRequired,
    resetForm: PropTypes.func.isRequired,
    suggestCustomers: PropTypes.func.isRequired,
  };

  static contextTypes = {
    history: PropTypes.object.isRequired,
  };

  constructor(props, context) {
    super(props, context);

    this.state = {
      checkoutAsGuest: false,
      customers: [],
      errors: [],
      query: '',
    };
  }

  componentDidMount() {
    this.props.suggestCustomers('');
  }

  shouldComponentUpdate(nextProps, nextState) {
    const cart = _.get(nextProps, 'newOrder.order.cart.referenceNumber');
    if (cart) {
      this.props.resetForm();
      transitionTo(this.context.history, 'order', { order: cart });
      return false;
    }

    return true;
  }

  get suggestedCustomers() {
    const empty = { rows: [], from: 0, size: 0, total: 0 };
    return _.get(this.props, 'newOrder.customers.results', empty);
  }

  get isFetching() {
    return _.get(this.props, 'newOrder.customers.results.isFetching', false);
  }

  get customersList() {
    return (
      <ChooseCustomer
        items={this.suggestedCustomers}
        onItemClick={this.selectCustomer}
        onGuestClick={this.submitGuest}
        isGuest={this.state.checkoutAsGuest} />
    );
  }

  get customers() {
    return this.state.customers.map(c => c.email);
  }

  get chooseCustomerInput() {
    return (
      <PilledInput
        solid={true}
        value={this.state.query}
        onChange={e => this.setState({query: e.target.value})}
        pills={this.customers}
        onPillClose={this.clearCustomer} />
    );
  }

  get nextButton() {
    return (
      <PrimaryButton type="submit"
                     onClick={this.submitAction}
                     className="fc-order-create__submit fc-btn fc-btn-primary fc-right" >
        Next
        <i className="icon-chevron-right" />
      </PrimaryButton>
    );
  }

  get search() {
    const label = this.state.checkoutAsGuest
      ? 'Guest Customer\'s Email'
      : 'Search All Customers';

    const placeholder = this.state.checkoutAsGuest
      ? 'Enter guest customer\'s email'
      : 'Customer name or email...';

    return (
      <Typeahead
        className="fc-order-create__customer-search fc-col-md-5-8"
        component={ChooseCustomerRow}
        fetchItems={(item) => this.props.suggestCustomers(item, this.state.checkoutAsGuest)}
        hideOnBlur={this.state.checkoutAsGuest}
        isFetching={this.isFetching}
        itemsElement={this.customersList}
        inputElement={this.chooseCustomerInput}
        label={label}
        onBlur={this.blur}
        placeholder={placeholder} />
    );
  }

  @autobind
  blur() {
    if (this.state.checkoutAsGuest) {
      this.submitGuest();
    }
  }

  @autobind
  submitGuest() {
    const guest = this.state.query;
    if (email(guest)) {
      this.setState({
        checkoutAsGuest: true
      }, () => this.selectCustomer({ email: guest }));
    } else if (!_.isEmpty(this.state.query) && _.isEmpty(this.state.customers)) {
      this.setState({
        checkoutAsGuest: true,
        errors: ['Please enter a valid email.'],
      });
    }
  }

  @autobind
  clearCustomer() {
    this.setState({
      customers: [],
    });
  }

  @autobind
  selectCustomer(customer) {
    this.setState({
      customers: [customer],
      errors: [],
      query: '',
    });
  }

  @autobind
  submitAction() {
    if (!_.isEmpty(this.state.customers)) {
      this.setState({ errors: [] });
      this.props.createOrder(this.state.customers[0], this.state.checkoutAsGuest);
    } else {
      this.setState({ errors: ['Please choose a customer.'] });
    }
  }

  @autobind
  toggleGuest(value) {
    this.setState({ checkoutAsGuest: value });
  }

  render() {
    return (
      <div className="fc-order-create">
        <div className="fc-grid">
          <PageTitle title="New Order" />
          <article className="fc-col-md-1-1">
            <div className="fc-grid fc-order-create__customer-form-panel">
              <div className="fc-order-create__customer-form-subtitle fc-col-md-1-1">
                <h2>Customer</h2>
              </div>
              <div className="fc-order-create__errors fc-col-md-1-1">
                <ErrorAlerts errors={this.state.errors} />
              </div>
              <div className="fc-order-create__customer-form fc-col-md-1-1">
                <Form
                  autoComplete="off"
                  className="fc-grid fc-grid-no-gutter">
                  {this.search}
                  <FormField
                    className="fc-order-create__guest-checkout fc-col-md-2-8"
                    label="Checkout as guest"
                    labelAfterInput={true}>
                    <BigCheckbox
                      id="guestCheckout"
                      value={this.state.checkoutAsGuest}
                      onToggle={this.toggleGuest} />
                  </FormField>
                  <FormField className="fc-col-md-1-8">
                    {this.nextButton}
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
