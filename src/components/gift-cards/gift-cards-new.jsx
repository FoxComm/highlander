import _ from 'lodash';
import { createSelector } from 'reselect';
import { autobind } from 'core-decorators';
import React, { PropTypes } from 'react';
import Counter from '../forms/counter';
import Typeahead from '../typeahead/typeahead';
import { Dropdown, DropdownItem }  from '../dropdown';
import { Checkbox } from '../checkbox/checkbox';
import { Link } from '../link';
import { connect } from 'react-redux';
import { Form, FormField } from '../forms';
import * as GiftCardNewActions from '../../modules/gift-cards/new';
import * as CustomersActions from '../../modules/customers/list';
import { createGiftCard } from '../../modules/gift-cards/cards';

const filterCustomers = createSelector(
  state => state.customers.items,
  ({giftCards: {adding}}) => adding.customersQuery,
  (customers, customersQuery) => _.filter(customers, customer => _.contains(customer.name, customersQuery))
);

const filterUsers = createSelector(
  state => state.customers.items,
  ({giftCards: {adding}}) => adding.usersQuery,
  (customers, usersQuery) => _.filter(customers, customer => _.contains(customer.name, usersQuery))
);

const subTypes = createSelector(
  ({giftCards: {adding}}) => adding.originType,
  ({giftCards: {adding}}) => adding.types,
  (originType, types) => types[originType]
);

const customerItem = props => <div>{props.item.name}</div>;

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
    console.log('change from', target);
    this.props.changeFormData(target.name,  target.checked != null ? target.checked : target.value);
  }

  changeCustomerMessage(event) {
    this.setState({customerMessageCount: event.target.value.length});
  }

  changeCSVMessage(event) {
    this.setState({csvMessageCount: event.target.value.length});
  }

  get subTypes() {
    const props = this.props;

    if (props.subTypes.length > 0) {
      return (
        <div className="fc-new-gift-card-subtypes fc-col-md-1-2">
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

  render() {
    const props = this.props;

    let
      customerSearch = null,
      quantity       = null,
      emailCSV       = null;

    const typeList = Object.keys(this.props.types);

    if (this.props.sendToCustomer) {
      customerSearch = (
        <div id="customerSearch">
          <Typeahead
            items={this.props.suggestedCustomers}
            fetchItems={this.props.suggestCustomers}
            component={customerItem}
            onItemSelected={this.props.addCustomer}
            label="Choose customers:"
            name="customerQuery"
          />
          <ul id="customerList">
            {this.props.customers.map((customer, idx) => {
              return (
                <li key={`customer-${idx}`}>
                  {customer.name}
                  <input type="hidden" name="customers[]" id={`customer_${idx}`} value={customer.id} />
                  <a onClick={() => this.props.removeCustomer(customer.id)}>&times;</a>
                </li>
              );
             })}
          </ul>
          <label htmlFor="customerMessage">Write a message for customers (optional):</label>
          <div className="counter">{this.state.customerMessageCount}/1000</div>
          <textarea name="customerMessage" maxLength="1000" onChange={this.changeCustomerMessage.bind(this)}></textarea>
        </div>
      );

      quantity = (
        <span>
          {this.props.customers.length} <input type="hidden" name="quantity" value={this.props.customers.length} />
        </span>
      );
    } else {
      quantity = <Counter inputName="quantity" />;
    }

    if (this.props.emailCSV) {
      emailCSV = (
        <div id="userSearch">
          <Typeahead
            items={this.props.suggestedUsers}
            fetchItems={this.props.suggestUsers}
            component={customerItem}
            onItemSelected={this.props.addUser}
            label="Choose users:"
            name="csvQuery"
          />
          <ul id="internalUserList">
            {this.props.users.map((user, idx) => {
              return (
                <li key={`user-${idx}`}>
                  {user.name}
                  <input type="hidden" name="users[]" id={`user_${idx}`} value={user.id} />
                  <a onClick={() => this.props.removeUser(user.id)}>&times;</a>
                </li>
              );
             })}
          </ul>
          <label htmlFor="internalMessage">Write a message (optional):</label>
          <div className="counter">{this.state.csvMessageCount}/1000</div>
          <textarea name="internalMessage" maxLength="1000" onChange={this.changeCSVMessage.bind(this)}></textarea>
        </div>
      );
    }

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
            <div className="fc-new-gift-card-types fc-col-md-1-2">
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
                    <div className="fc-new-gift-card-balance-value" key={`balance-${idx}`}
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
              <Checkbox id="sendToCustomer" name="sendToCustomer"  value={this.props.sendToCustomer} />
              Send gift card(s) to customer(s)
            </label>
            { customerSearch }
          </fieldset>
          <fieldset>
            <label htmlFor="quantity">Quantity</label>
            {quantity}
          </fieldset>
          <fieldset>
            A CSV file of the gift cards can be created. What would you like to do?
            <label htmlFor="download_csv">
              <input type="checkbox" name="download_csv" />
              Download CSV file immediately after it is created.
            </label>
            <label htmlFor="email_csv">
              <input type="checkbox" name="emailCSV" value={this.props.emailCSV}/>
              Email the CSV file.
            </label>
            { emailCSV }
          </fieldset>

          <Link to='gift-cards' className="fc-btn-link">Cancel</Link>
          <button className="fc-btn fc-btn-primary" type="submit">Issue Gift Card</button>

        </form>
      </div>
    );
  }
}
