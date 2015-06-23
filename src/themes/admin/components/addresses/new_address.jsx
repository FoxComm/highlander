'use strict';

import React from 'react';
import AddressStore from './store';

export default class NewAddress extends React.Component {
  handleChanges(e) {
    let field = {},
      target = e.target;
    field[target.name] = target.value;
    this.setState(field);
  }

  onSubmitForm(e) {
    e.preventDefault();

    console.log(this.state);
    // Todo (cam-stitt): Alter this.state to be json of form.
    AddressStore.create(this.state);
  }

  render() {
    return (
      <form method='POST' onSubmit={this.onSubmitForm.bind(this)}>
        <div>
          <label htmlFor="name">Name</label>
          <input type="text" name="name" onChange={this.handleChanges.bind(this)} required />
        </div>
        <div>
          <label htmlFor="street1">Address 1</label>
          <input type="text" name="street1" onChange={this.handleChanges.bind(this)} required />
        </div>
        <div>
          <label htmlFor="street2">Address 2 (option)</label>
          <input type="text" name="street2" onChange={this.handleChanges.bind(this)} />
        </div>
        <div>
          <label htmlFor="country">Country</label>
          <input type="text" name="country" onChange={this.handleChanges.bind(this)} required />
        </div>
        <div>
          <label htmlFor="state">State</label>
          <input type="text" name="state" onChange={this.handleChanges.bind(this)} required />
        </div>
        <div>
          <label htmlFor="city">City</label>
          <input type="text" name="city" onChange={this.handleChanges.bind(this)} required />
        </div>
        <div>
          <label htmlFor="zip">Zip</label>
          <input type="number" name="zip" onChange={this.handleChanges.bind(this)} required />
        </div>
        <div>
          <label htmlFor="phone">Phone</label>
          <input type="tel" name="phone" onChange={this.handleChanges.bind(this)} required />
        </div>
        <div>
          <button type='submit'>Submit</button>
        </div>
      </form>
    );
  }
}
