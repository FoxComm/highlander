'use strict';

import React from 'react';
import Counter from '../forms/counter';
import Typeahead from '../typeahead/typeahead';
import CustomerResult from '../customers/result';
import CustomerStore from '../customers/store';
import { listenTo, stopListeningTo } from '../../lib/dispatcher';

const
  types = {
    Appeasement: [],
    Marketing: ['One', 'Two']
  },
  selectEvent = 'gift-card-customer-selected';

export default class NewGiftCard extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      amount: '0.00',
      type: 'Appeasement',
      subTypes: types.Appeasement,
      sendToCustomer: false,
      customers: []
    };
  }

  componentDidMount() {
    listenTo(selectEvent, this);
  }

  componentWillUnMount() {
    stopListeningTo(selectEvent, this);
  }

  onChangeValue(event) {
    this.setState({
      amount: event.target.value
    });
  }

  setValue(value) {
    this.setState({
      amount: value
    });
  }

  setType(event) {
    this.setState({
      type: event.target.value,
      subTypes: types[event.target.value]
    });
  }

  sendToCustomer() {
    let customerList = this.state.sendToCustomer ? [] : this.state.customers;
    this.setState({
      sendToCustomer: !this.state.sendToCustomer,
      customers: customerList
    });
  }

  onGiftCardCustomerSelected(customer) {
    let customerList = this.state.customers;

    var exists = customerList.filter(function (item) {
      return item.id === customer.id;
    }).length > 0;

    if (!exists) {
      customerList.push(customer);
    }

    this.setState({
      customers: customerList
    });
  }

  render() {
    let
      typeList       = Object.keys(types),
      subTypeContent = null,
      customerSearch = null;

    if (this.state.subTypes.length > 0) {
      subTypeContent = (
        <div>
          <label htmlFor="cardSubType">Subtype</label>
          <select name="cardSubType">
            {this.state.subTypes.map((subType) => {
              return <option val={subType}>{subType}</option>;
             })};
          </select>
        </div>
      );
    }

    if (this.state.sendToCustomer) {
      customerSearch = (
        <div>
          <Typeahead store={CustomerStore} component={CustomerResult} selectEvent="giftCardCustomerSelected" />
          <ul id="customerList">
            {this.state.customers.map((customer, idx) => {
              return (
                <li key={`customer-${customer.id}`}>
                  {customer.firstName} {customer.lastName}
                  <input type="hidden" name="customers[]" id={`customer_${idx}`} value={customer.id} />
                </li>
              );
            })}
          </ul>
        </div>
      );
    }

    return (
      <div id="new-gift-card" className="gutter">
        <h2>New Gift Cards</h2>
        <form action="/gift-cards" method="POST" className="vertical">
          <fieldset>
            <fieldset>
              <label htmlFor="cardType">Gift Card Type</label>
              <select name="cardType" onChange={this.setType.bind(this)}>
                {typeList.map((type, idx) => {
                  return <option val={type} key={`${idx}-${type}`}>{type}</option>;
                })}
              </select>
              {subTypeContent}
            </fieldset>
            <fieldset>
              <label htmlFor="value">Value</label>
              <div className="form-icon">
                <i className="icon-dollar"></i>
                <input type="number" name="value" value={this.state.amount} onChange={this.onChangeValue.bind(this)} />
              </div>
              <div>
                <a className="btn" onClick={this.setValue.bind(this, '10.00')}>$10</a>
                <a className="btn" onClick={this.setValue.bind(this, '25.00')}>$25</a>
                <a className="btn" onClick={this.setValue.bind(this, '50.00')}>$50</a>
                <a className="btn" onClick={this.setValue.bind(this, '100.00')}>$100</a>
                <a className="btn" onClick={this.setValue.bind(this, '200.00')}>$200</a>
              </div>
            </fieldset>
            <fieldset>
              <label htmlFor="sendToCustomer">
                <input type="checkbox" name="sendToCustomer" value={this.state.sendToCustomer} onChange={this.sendToCustomer.bind(this)} />
                Send gift cards to customers?
              </label>
              { customerSearch }
            </fieldset>
            <fieldset>
              <label htmlFor="quantity">Quantity</label>
              <Counter inputName="quantity" />
            </fieldset>
          </fieldset>
        </form>
      </div>
    );
  }
}
