'use strict';

import React from 'react';

export default class NewGiftCard extends React.Component {
  render() {
    return (
      <div id="new-gift-card" className="gutter">
        <h2>New Gift Cards</h2>
        <form action="/gift-cards" method="POST" className="vertical">
          <fieldset>
            <fieldset>
              <label htmlFor="cardType">Gift Card Type</label>
              <select name="cardType">
              </select>
              <label htmlFor="cardSubType">Subtype</label>
              <select name="cardSubType">
              </select>
            </fieldset>
            <fieldset>
              <label htmlFor="value">Value</label>
              <div className="form-icon">
                <i className="icon-"></i>
                <input type="number" name="value" />
              </div>
              <div>
                <a className="btn" data-amount="10.00">$10</a>
                <a className="btn" data-amount="10.00">$25</a>
                <a className="btn" data-amount="10.00">$50</a>
                <a className="btn" data-amount="10.00">$100</a>
                <a className="btn" data-amount="10.00">$200</a>
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
