'use strict';

import React from 'react';
import Counter from '../forms/counter';
import Typeahead from '../typeahead/typeahead';
import CustomerResult from '../customers/result';
import CustomerStore from '../../stores/customers';
import _ from 'lodash';
import { Link } from 'react-router';
import GiftCardActions from '../../actions/gift-cards';

const types = {
  Appeasement: [],
  Marketing: ['One', 'Two']
};

export default class NewGiftCard extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.state = {
      balance: 100,
      balanceText: '1.00',
      originType: 'Appeasement',
      subTypes: types.Appeasement,
      sendToCustomer: false,
      customers: [],
      users: [],
      emailCSV: false,
      customerMessageCount: 0,
      csvMessageCount: 0
    };
  }

  onChangeBalance(event) {
    this.setState({
      balance: (event.target.value * 100) | 0,
      balanceText: event.target.value
    });
  }

  setValue(value) {
    this.setState({
      balance: value,
      balanceText: (value / 100).toFixed(2)
    });
  }

  setType(event) {
    this.setState({
      originType: event.target.value,
      subTypes: types[event.target.value]
    });
  }

  toggleSendToCustomer() {
    let customerList = this.state.sendToCustomer ? [] : this.state.customers;
    this.setState({
      sendToCustomer: !this.state.sendToCustomer,
      customers: customerList
    });
  }

  toggleEmailCSV() {
    let userList = this.state.emailCSV ? [] : this.state.users;
    this.setState({
      emailCSV: !this.state.emailCSV,
      users: userList
    });
  }

  onGiftCardCustomerSelected(customer) {
    let
      customerList = this.state.customers.slice(0, this.state.customers.length),
      existing = _(customerList).find(function (item) {
        return item.id === customer.id;
      });

    if (existing) return;

    customerList.push(customer);
    this.setState({
      customers: customerList
    });
  }

  onEmailCsvUserSelected(user) {
    let
      userList = this.state.users.slice(0, this.state.users.length),
      existing = _(userList).find(function (item) {
        return item.id === user.id;
      });

    if (existing) return;

    userList.push(user);
    this.setState({
      users: userList
    });
  }

  submitForm(event) {
    event.preventDefault();

    GiftCardActions.createGiftCard(event.target);
  }

  closeForm(cards) {
    cards = cards || [];
  }

  removeCustomer(idx) {
    let customerList = this.state.customers.slice(0, this.state.users.length);

    customerList.splice(idx, 1);
    this.setState({customers: customerList});
  }

  removeUser(idx) {
    let userList = this.state.users.slice(0, this.state.users.length);

    userList.splice(idx, 1);
    this.setState({users: userList});
  }

  changeCustomerMessage(event) {
    this.setState({customerMessageCount: event.target.value.length});
  }

  changeCSVMessage(event) {
    this.setState({csvMessageCount: event.target.value.length});
  }

  render() {
    let
      typeList       = Object.keys(types),
      subTypeContent = null,
      customerSearch = null,
      quantity       = null,
      emailCSV       = null;

    if (this.state.subTypes.length > 0) {
      subTypeContent = (
        <div id="subTypes">
          <label htmlFor="cardSubType">Subtype</label>
          <select name="cardSubType">
            {this.state.subTypes.map((subType, idx) => {
              return <option key={`subType-${idx}`} val={subType}>{subType}</option>;
             })};
          </select>
        </div>
      );
    }

    if (this.state.sendToCustomer) {
      customerSearch = (
        <div id="customerSearch">
          <Typeahead
            store={CustomerStore}
            component={CustomerResult}
            callback={this.onGiftCardCustomerSelected.bind(this)}
            label="Choose customers:"
            name="customerQuery"
          />
          <ul id="customerList">
            {this.state.customers.map((customer, idx) => {
              return (
                <li key={`customer-${customer.id}`}>
                  {customer.firstName} {customer.lastName}
                  <input type="hidden" name="customers[]" id={`customer_${idx}`} value={customer.id} />
                  <a onClick={this.removeCustomer.bind(this, idx)}>&times;</a>
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
          {this.state.customers.length} <input type="hidden" name="quantity" value={this.state.customers.length} />
        </span>
      );
    } else {
      quantity = <Counter inputName="quantity" />;
    }

    if (this.state.emailCSV) {
      emailCSV = (
        <div id="userSearch">
          <Typeahead
            store={CustomerStore}
            component={CustomerResult}
            callback={this.onEmailCsvUserSelected.bind(this)}
            label="Choose users:"
            name="csvQuery"
          />
          <ul id="internalUserList">
            {this.state.users.map((user, idx) => {
              return (
                <li key={`user-${user.id}`}>
                  {user.firstName} {user.lastName}
                  <input type="hidden" name="users[]" id={`user_${idx}`} value={user.id} />
                  <a onClick={this.removeUser.bind(this, idx)}>&times;</a>
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
      <div id="new-gift-card" className="gutter">
        <h2>Issue New Gift Cards</h2>
        <form action="/gift-cards" method="POST" className="vertical" onSubmit={this.submitForm.bind(this)}>
          <fieldset>
            <div id="cardTypes">
              <label htmlFor="originType">Gift Card Type</label>
              <select name="originType" onChange={this.setType.bind(this)}>
                {typeList.map((type, idx) => {
                  return <option val={type} key={`${idx}-${type}`}>{type}</option>;
                 })}
              </select>
            </div>
            {subTypeContent}
          </fieldset>
          <fieldset>
            <label htmlFor="value">Value</label>
            <div className="fc-input-group">
              <div className="fc-input-prepend"><i className="icon-usd"></i></div>
              <input type="hidden" name="originalBalance" value={this.state.balance} />
              <input type="number" value={this.state.balanceText} step="0.01" min="1"
                     onChange={this.onChangeBalance.bind(this)}
              />
            </div>
            <div id="balances">
              <a className="btn" onClick={this.setValue.bind(this, '1000')}>$10</a>
              <a className="btn" onClick={this.setValue.bind(this, '2500')}>$25</a>
              <a className="btn" onClick={this.setValue.bind(this, '5000')}>$50</a>
              <a className="btn" onClick={this.setValue.bind(this, '10000')}>$100</a>
              <a className="btn" onClick={this.setValue.bind(this, '20000')}>$200</a>
            </div>
          </fieldset>
          <fieldset>
            <label htmlFor="sendToCustomer" className="checkbox">
              <input type="checkbox" name="sendToCustomer" value={this.state.sendToCustomer}
                     onChange={this.toggleSendToCustomer.bind(this)}
              />
              Send gift cards to customers?
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
              <input type="checkbox" name="email_csv" value={this.state.emailCSV}
                     onChange={this.toggleEmailCSV.bind(this)}
              />
              Email the CSV file.
            </label>
            { emailCSV }
          </fieldset>
          <Link to='gift-cards' className="fc-btn-link">Cancel</Link>
          <input type="submit" value="Issue Gift Card" />
        </form>
      </div>
    );
  }
}
