'use strict';

import React from 'react';
import AddressStore from './store';
import { dispatch } from '../../lib/dispatcher';

export default class NewAddress extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isActive: true,
      country: 'US',
      state: 'CA'
    };
  }

  handleChanges(e) {
    let
      field = {},
      target = e.target;
    field[target.name] = target.value;
    this.setState(field);
  }

  onSubmitForm(e) {
    e.preventDefault();

    // Todo (cam-stitt): Alter this.state to be json of form.
    AddressStore.create(this.state);
  }

  cancelAddress() {
    dispatch('cancelNewAddress');
  }

  render() {
    return (
      <form className='vertical' method='POST' onSubmit={this.onSubmitForm.bind(this)}>
        <div>
          <label htmlFor="name">Name</label>
          <input type="text" name="name" className='control' onChange={this.handleChanges.bind(this)} required />
        </div>
        <div>
          <label htmlFor="street1">Address 1</label>
          <input type="text" name="street1" className='control' onChange={this.handleChanges.bind(this)} required />
        </div>
        <div>
          <label htmlFor="street2">Address 2 (option)</label>
          <input type="text" name="street2" className='control' onChange={this.handleChanges.bind(this)} />
        </div>
        <div>
          <label htmlFor="country">Country</label>
          <select name='country' onChange={this.handleChanges.bind(this)}>
            <option val='US'>United States</option>
          </select>
        </div>
        <div>
          <label htmlFor="state">State</label>
          <select name='state' onChange={this.handleChanges.bind(this)}>
            <option val='CA'>California</option>
          </select>
        </div>
        <div>
          <label htmlFor="city">City</label>
          <input type="text" name="city" className='control' onChange={this.handleChanges.bind(this)} required />
        </div>
        <div>
          <label htmlFor="zip">Zip</label>
          <input type="number" name="zip" className='control' onChange={this.handleChanges.bind(this)} required />
        </div>
        <div>
          <label htmlFor="phone">Phone</label>
          <input type="tel" name="phone" className='control' onChange={this.handleChanges.bind(this)} required />
        </div>
        <div>
          <a onClick={this.cancelAddress}>Cancel</a>
          <button type='submit' className='btn'>Submit</button>
        </div>
      </form>
    );
  }
}
