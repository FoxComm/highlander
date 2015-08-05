'use strict';

import React from 'react';
import AddressStore from './store';
import { dispatch } from '../../lib/dispatcher';
import Api from '../../lib/api';

export default class NewAddress extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      states: []
    };
  }

  componentDidMount() {
    Api.get('/states')
       .then((res) => {
         this.setState({
           states: res
         });
       })
       .catch((err) => { console.log(err); });
  }

  onSubmitForm(event) {
    event.preventDefault();

    Api.submitForm(event.target)
       .then((res) => {
         AddressStore.update(res);
       })
       .catch((err) => { console.log(err); });
  }

  cancelAddress() {
    dispatch('cancelNewAddress');
  }

  render() {
    return (
      <form action={`/customers/addresses`} className='vertical' method='POST' onSubmit={this.onSubmitForm.bind(this)}>
        <div>
          <label htmlFor="name">Name</label>
          <input type="text" name="name" className='control' required />
        </div>
        <div>
          <label htmlFor="street1">Address 1</label>
          <input type="text" name="street1" className='control' required />
        </div>
        <div>
          <label htmlFor="street2">Address 2 (option)</label>
          <input type="text" name="street2" className='control' />
        </div>
        <div>
          <label htmlFor="state">State</label>
          <select name='stateId'>
            {this.state.states.map((state) => {
              return <option value={state.id}>{state.name}</option>;
             })}
          </select>
        </div>
        <div>
          <label htmlFor="city">City</label>
          <input type="text" name="city" className='control' required />
        </div>
        <div>
          <label htmlFor="zip">Zip</label>
          <input type="number" name="zip" className='control' required />
        </div>
        <div>
          <label htmlFor="phone">Phone</label>
          <input type="tel" name="phone" className='control' required />
        </div>
        <div>
          <a onClick={this.cancelAddress}>Cancel</a>
          <input type='submit' className='btn' value="Submit" />
        </div>
      </form>
    );
  }
}
