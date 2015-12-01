
// libs
import _ from 'lodash';
import { createSelector } from 'reselect';
import { autobind } from 'core-decorators';
import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

// components
import Counter from '../forms/counter';
import Typeahead from '../typeahead/typeahead';
import { Dropdown, DropdownItem }  from '../dropdown';
import { Checkbox } from '../checkbox/checkbox';
import { Link } from '../link';
import { Form, FormField } from '../forms';
import ChooseCustomers from './choose-customers';

// redux
import * as GiftCardNewActions from '../../modules/gift-cards/new';
import * as CustomersActions from '../../modules/customers/list';
import { createGiftCard } from '../../modules/gift-cards/cards';

const selectCustomersList = state => _.get(state, ['customers', 'customers', 'rows'], []);

const filterCustomers = createSelector(
  selectCustomersList,
  ({giftCards: {adding}}) => adding.customersQuery,
  (customers, customersQuery) => _.filter(customers, customer => _.contains(customer.name, customersQuery))
);

const filterUsers = createSelector(
  selectCustomersList,
  ({giftCards: {adding}}) => adding.usersQuery,
  (customers, usersQuery) => _.filter(customers, customer => _.contains(customer.name, usersQuery))
);

const subTypes = createSelector(
  ({giftCards: {adding}}) => adding.originType,
  ({giftCards: {adding}}) => adding.types,
  (originType, types) => types[originType]
);

@connect(state => ({
  ...state.giftCards.adding,
  suggestedCustomers: filterCustomers(state),
  suggestedUsers: filterUsers(state),
  subTypes: subTypes(state)
}), {
  ...GiftCardNewActions,
  fetchCustomers: CustomersActions.fetch,
  createGiftCard
})
export default class NewGiftCard extends React.Component {

  static propTypes = {
    addCustomer: PropTypes.func,
    addUser: PropTypes.func,
    balance: PropTypes.number,
    balanceText: PropTypes.string,
    changeFormData: PropTypes.func.isRequired,
    createGiftCard: PropTypes.func.isRequired,
    customers: PropTypes.map,
    emailCSV: PropTypes.bool,
    fetchCustomers: PropTypes.func.isRequired,
    removeCustomer: PropTypes.func,
    removeUser: PropTypes.func,
    sendToCustomer: PropTypes.bool,
    subTypes: PropTypes.map,
    suggestCustomers: PropTypes.func,
    suggestedCustomers: PropTypes.array,
    suggestUsers: PropTypes.func,
    suggestedUsers: PropTypes.array,
    types: PropTypes.object,
    users: PropTypes.map
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      customerMessageCount: 0,
      csvMessageCount: 0
    };
  }

  componentDidMount() {
    this.props.fetchCustomers();
  }

  @autobind
  submitForm(event) {
    event.preventDefault();
    this.props.createGiftCard();
  }

  @autobind
  onChangeValue({target}) {
    const value = target.type === 'checkbox' ? target.checked : target.value;

    this.props.changeFormData(target.name,  value);
  }

  changeCustomerMessage(event) {
    this.setState({customerMessageCount: event.target.value.length});
  }

  get subTypes() {
    const props = this.props;

    if (props.subTypes.length > 0) {
      return (
        <div className="fc-new-gift-card__subtypes fc-col-md-1-2">
          <label htmlFor="cardSubType">Subtype</label>
          <Dropdown value={props.subTypes[0]} onChange={ value => props.changeFormData('cardSubType', value) }>
            {props.subTypes.map((subType, idx) => {
              return <DropdownItem key={`subType-${idx}`} value={subType}>{subType}</DropdownItem>;
              })}
          </Dropdown>
        </div>
      );
    }
  }

  get customerListBlock() {
    if (this.props.sendToCustomer) {
      const labelAtRight = <div className="fc-new-gift-card__counter">{this.state.customerMessageCount}/1000</div>;

      return (
        <div className="fc-new-gift-card__send-to-customers">
          <Typeahead
            className="_no-search-icon"
            items={this.props.suggestedCustomers}
            fetchItems={this.props.suggestCustomers}
            itemsComponent={ChooseCustomers}
            onItemSelected={(_, event) => event.preventHiding()}
            label="Choose customers:"
            name="customerQuery"
          />
          <ul id="customerList">
            {this.props.customers.map((customer, idx) => {
              return (
                <li key={`customer-${idx}`}>
                  {customer.name}
                  <input type="hidden" name="customers[]" id={`customer_${idx}`} value={customer.id}/>
                  <a onClick={() => this.props.removeCustomer(customer.id)}>&times;</a>
                </li>
              );
            })}
          </ul>

          <FormField className="fc-new-gift-card__message-to-customers"
                     label="Write a message for customers" optional
                     labelAtRight={ labelAtRight }>
            <textarea className="fc-input" name="customerMessage" maxLength="1000" onChange={this.changeCustomerMessage.bind(this)}></textarea>
          </FormField>
        </div>
      );
    }
  }

  get quantitySection() {
    if (!this.props.sendToCustomer) {

      const changeQuantity = (event, amount) => {
        event.preventDefault();
        this.props.changeQuantity(this.props.quantity + amount);
      };

      return (
        <fieldset>
          <label htmlFor="quantity">Quantity</label>
          <Counter
            id="quantity"
            value={this.props.quantity}
            increaseAction={event => changeQuantity(event, 1)}
            decreaseAction={event => changeQuantity(event, -1)}
            onChange={({target}) => this.props.changeQuantity(target.value)}
            min={1} />
        </fieldset>
      );
    }
  }

  render() {
    const props = this.props;
    const typeList = Object.keys(this.props.types);

    return (
      <div className="fc-new-gift-card fc-grid">
        <header className="fc-col-md-1-1">
          <h1>Issue New Gift Card</h1>
        </header>
        <form action="/gift-cards"
              method="POST"
              className="fc-form-vertical fc-col-md-1-1"
              onSubmit={this.submitForm}
              onChange={this.onChangeValue}>
          <div className="fc-grid fc-grid-no-gutter">
            <div className="fc-new-gift-card__types fc-col-md-1-2">
              <label htmlFor="originType">Gift Card Type</label>
              <Dropdown value={typeList[0]} onChange={value => props.changeFormData('originType', value) }>
                {typeList.map((type, idx) => {
                  return <DropdownItem value={type} key={`${idx}-${type}`}>{type}</DropdownItem>;
                 })}
              </Dropdown>
            </div>
            {this.subTypes}
          </div>
          <fieldset>
            <label htmlFor="value">Value</label>
            <div className="fc-input-group">
              <div className="fc-input-prepend"><i className="icon-usd"></i></div>
              <input type="hidden" name="balance" value={this.props.balance} />
              <input type="number" name="balanceText" value={this.props.balanceText} step="0.01" min="1"/>
            </div>
            <div id="balances">
              {
                [1000, 2500, 5000, 10000, 20000].map((balance, idx) => {
                  return (
                    <div className="fc-new-gift-card__balance-value" key={`balance-${idx}`}
                         onClick={() => this.props.changeFormData('balance', balance)}>

                      ${balance/100}
                    </div>
                  );
                })
              }
            </div>
          </fieldset>
          <fieldset>
            <label>
              <Checkbox id="sendToCustomer" name="sendToCustomer" checked={this.props.sendToCustomer} />
              Send gift card(s) to customer(s)
            </label>
            { this.customerListBlock }
          </fieldset>
          {this.quantitySection}
          <div className="fc-action-block">
            <Link to='gift-cards' className="fc-btn-link fc-action-block-cancel">Cancel</Link>
            <button className="fc-btn fc-btn-primary" type="submit">Issue Gift Card</button>
          </div>
        </form>
      </div>
    );
  }
}
