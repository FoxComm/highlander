'use strict';

import React from 'react';
import Counter from '../forms/counter';
import Typeahead from '../typeahead/typeahead';
import CustomerResult from '../customers/result';
import CustomerStore from '../customers/store';
import { dispatch, listenTo, stopListeningTo } from '../../lib/dispatcher';
import Api from '../../lib/api';

const
  types = {
    Appeasement: [],
    Marketing: ['One', 'Two']
  },
  customerSelectEvent = 'gift-card-customer-selected',
  userSelectEvent = 'email-csv-user-selected';

export default class NewGiftCard extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      balance: '0.00',
      type: 'Appeasement',
      subTypes: types.Appeasement,
      sendToCustomer: false,
      customers: [],
      users: [],
      emailCSV: false,
      customerMessageCount: 0,
      csvMessageCount: 0
    };
  }

  componentDidMount() {
    listenTo(customerSelectEvent, this);
    listenTo(userSelectEvent, this);
  }

  componentWillUnMount() {
    stopListeningTo(customerSelectEvent, this);
    stopListeningTo(userSelectEvent, this);
  }

  onChangeValue(event) {
    this.setState({
      balance: event.target.value
    });
  }

  setValue(value) {
    this.setState({
      balance: value
    });
  }

  setType(event) {
    this.setState({
      type: event.target.value,
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
      customerList = this.state.customers,
      exists = customerList.filter(function (item) {
        return item.id === customer.id;
      }).length > 0;

    if (!exists) {
      customerList.push(customer);
    }

    this.setState({
      customers: customerList
    });
  }

  onEmailCsvUserSelected(user) {
    let
      userList = this.state.users,
      exists = userList.filter(function (item) {
        return item.id === user.id;
      }).length > 0;

    if (!exists) {
      userList.push(user);
    }

    this.setState({
      users: userList
    });
  }

  submitForm(event) {
    event.preventDefault();

    Api.submitForm(event.target)
       .then((res) => {
         this.closeForm(res);
       })
       .catch((err) => { console.log(err); });
  }

  closeForm(cards) {
    cards = cards || [];
    dispatch('cardsAdded', cards);
  }

  removeCustomer(idx) {
    let customerList = this.state.customers;

    customerList.splice(idx, 1);
    this.setState({customers: customerList});
  }

  removeUser(idx) {
    let userList = this.state.users;

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
          <Typeahead store={CustomerStore} component={CustomerResult} selectEvent="giftCardCustomerSelected" label="Choose customers:" name="customerQuery" />
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

      quantity = <span>{this.state.customers.length} <input type="hidden" name="quantity" value={this.state.customers.length} /></span>;
    } else {
      quantity = <Counter inputName="quantity" />;
    }

    if (this.state.emailCSV) {
      emailCSV = (
        <div id="userSearch">
          <Typeahead store={CustomerStore} component={CustomerResult} selectEvent="emailCsvUserSelected" label="Choose users:" name="csvQuery" />
          <ul id="internalUserList">
            {this.state.users.map((user, idx) => {
              return (
                <li key={`user-${user.id}`}>
                  {user.firstName} {user.lastName}
                  <input type="hidden" name="users[]" id={`user_${idx}`} value={user.id} />
                  <a onClock={this.removeUser.bind(this, idx)}>&times;</a>
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
              <label htmlFor="cardType">Gift Card Type</label>
              <select name="cardType" onChange={this.setType.bind(this)}>
                {typeList.map((type, idx) => {
                  return <option val={type} key={`${idx}-${type}`}>{type}</option>;
                 })}
              </select>
            </div>
            {subTypeContent}
          </fieldset>
          <fieldset>
            <label htmlFor="value">Value</label>
            <div className="form-icon">
              <i className="icon-dollar"></i>
              <input type="number" className="control" name="balance" value={this.state.balance} onChange={this.onChangeValue.bind(this)} />
            </div>
            <div id="balances">
              <a className="btn" onClick={this.setValue.bind(this, '10.00')}>$10</a>
              <a className="btn" onClick={this.setValue.bind(this, '25.00')}>$25</a>
              <a className="btn" onClick={this.setValue.bind(this, '50.00')}>$50</a>
              <a className="btn" onClick={this.setValue.bind(this, '100.00')}>$100</a>
              <a className="btn" onClick={this.setValue.bind(this, '200.00')}>$200</a>
            </div>
          </fieldset>
          <fieldset>
            <label htmlFor="sendToCustomer" className="checkbox">
              <input type="checkbox" name="sendToCustomer" value={this.state.sendToCustomer} onChange={this.toggleSendToCustomer.bind(this)} />
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
              <input type="checkbox" name="email_csv" value={this.state.emailCSV} onChange={this.toggleEmailCSV.bind(this)} />
              Email the CSV file.
            </label>
            { emailCSV }
          </fieldset>
          <a onClick={this.closeForm.bind(this)}>Cancel</a>
          <input type="submit" value="Issue Gift Card" />
        </form>
      </div>
    );
  }
}
