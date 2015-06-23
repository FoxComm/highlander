'use strict';

import React from 'react';
import AddressStore from './store';

export default class NewAddress extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      isActive: true
    };
  }

  handleChanges(e) {
    let field = {},
      target = e.target;
    field[target.name] = target.value;
    this.setState(field);
  }

  onSubmitForm(e) {
    e.preventDefault();

    // Todo (cam-stitt): Alter this.state to be json of form.
    AddressStore.create(this.state);
  }

  render() {
    return (
      <form className='vertical gutter' method='POST' onSubmit={this.onSubmitForm.bind(this)}>
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
          <input type="text" name="country" className='control' onChange={this.handleChanges.bind(this)} required />
        </div>
        <div>
          <label htmlFor="state">State</label>
          <input type="text" name="state" className='control' onChange={this.handleChanges.bind(this)} required />
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
          <button type='submit' className='btn'>Submit</button>
        </div>
      </form>
    );
  }
}
