'use strict';

import React from 'react';

const
  types = ['Appeasement', 'Marketing'],
  subTypes = [
    [],
    ['One', 'Two']
  ];

export default class NewGiftCard extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      amount: '0.00',
      type: types[0]
    };
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
      type: types[+event.target.value]
    });
  }

  render() {
    let
      subTypeContent = null,
      typeIdx        = types.indexOf(this.state.type),
      typesSubTypes  = null;

    if (typeIdx > -1) {
      typeSubTypes = subTypes[typeIdx];
      subTypeContent = (
        <div>
          <label htmlFor="cardSubType">Subtype</label>
          <select name="cardSubType">
            {typeSubTypes.map((subType, idx) => {
              return <option val={subType}>{subType}</option>;
             })};
          </select>
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
                {types.map((type, idx) => {
                  return <option val={idx} key={`${idx}-${type}`}>{type}</option>;
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
                <input type="checkbox" name="sendToCustomer" />
                Send gift cards to customers?
              </label>
            </fieldset>
          </fieldset>
        </form>
      </div>
    );
  }
}
