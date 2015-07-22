'use strict';

import React from 'react';

export default class NewGiftCard extends React.Component {
  render() {
    return (
      <div id="new-gift-card">
        <form action="/gift-cards" method="POST">
          <fieldset>
            <legend>Issue New Gift Card</legend>
            <fieldset>
              <label for="cardType">Gift Card Type</label>
              <select name="cardType">

              </select>
            </fieldset>
            <fieldset>
              <label for="cardSubType">Subtype</label>
              <select name="cardSubType">
              </select>
            </fieldset>
            <fieldset>
              <label for="value">Value</label>
              <input type="number" name="value" />
              <a className="btn" data-amount="10.00">$10</a>
              <a className="btn" data-amount="10.00">$25</a>
              <a className="btn" data-amount="10.00">$50</a>
              <a className="btn" data-amount="10.00">$100</a>
              <a className="btn" data-amount="10.00">$200</a>
            </fieldset>
          </fieldset>
        </form>
      </div>
    );
  }
}
