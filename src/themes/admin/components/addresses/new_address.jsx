'use strict';

import React from 'react';

export default class NewAddress extends React.Component {
  render() {
    return (
      <form method='POST' action='/api/v1/addresses'>
        <div>
          <label htmlFor="firstName">First Name</label>
          <input type="text" name="firstName" />
        </div>
        <div>
          <label htmlFor="lastName">Last Name</label>
          <input type="text" name="lastName" />
        </div>
        <div>
          <label htmlFor="street1">Address 1</label>
          <input type="text" name="street1" />
        </div>
        <div>
          <label htmlFor="street2">Address 2 (option)</label>
          <input type="text" name="street2" />
        </div>
        <div>
          <label htmlFor="country">Country</label>
          <input type="text" name="firstName" />
        </div>
        <div>
          <label htmlFor="state">State</label>
          <input type="text" name="firstName" />
        </div>
        <div>
          <label htmlFor="city">City</label>
          <input type="text" name="city" />
        </div>
        <div>
          <label htmlFor="zip">Zip</label>
          <input type="number" name="zip" />
        </div>
        <div>
          <label htmlFor="phone">Phone</label>
          <input type="tel" name="phone" />
        </div>
        <div>
          <button type='submit'>Submit</button>
        </div>
      </form>
    );
  }
}
