'use strict';

import React from 'react';
import AddressStore from './store';
import { dispatch } from '../../lib/dispatcher';
import Api from '../../lib/api';
import _ from 'underscore';

export default class NewAddress extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      states: [],
      formData: {}
    };
  }

  componentDidMount() {
    let formData = _.extend({}, this.state.formData);

    Api.get('/states')
       .then((res) => {
         formData.stateId = res[0].id;
         this.setState({
           states: res,
           formData: formData
         });
       })
       .catch((err) => { console.log(err); });
  }

  onSubmitForm(event) {
    event.preventDefault();

    Api.post(event.target.getAttribute('action'), this.state.formData)
       .then((res) => {
         AddressStore.update(res);
       })
       .catch((err) => { console.log(err); });
  }

  onChangeValue(event) {
    let
      formData = _.extend({}, this.state.formData),
      target = event.target;

    if (target.name === 'stateId') {
      formData[target.name] = +target.value;
    } else {
      formData[target.name] = target.value;
    }

    this.setState({
      formData: formData
    });
  }

  cancelAddress() {
    dispatch('cancelNewAddress');
  }

  render() {
    return (
      <form action={`/customers/${this.props.customerId}/addresses`} className='vertical' method='POST' onSubmit={this.onSubmitForm.bind(this)} onChange={this.onChangeValue.bind(this)}>
        <div>
          <label htmlFor="name">Name</label>
          <input type="text" name="name" required />
        </div>
        <div>
          <label htmlFor="street1">Address 1</label>
          <input type="text" name="street1" required />
        </div>
        <div>
          <label htmlFor="street2">Address 2 (option)</label>
          <input type="text" name="street2" />
        </div>
        <div>
          <label htmlFor="stateId">State</label>
          <select name="stateId">
            {this.state.states.map((state) => {
              return <option value={state.id}>{state.name}</option>;
             })}
          </select>
        </div>
        <div>
          <label htmlFor="city">City</label>
          <input type="text" name="city" required />
        </div>
        <div>
          <label htmlFor="zip">Zip</label>
          <input type="text" name="zip" required />
        </div>
        <div>
          <label htmlFor="phoneNumber">Phone</label>
          <input type="tel" name="phoneNumber" required />
        </div>
        <div>
          <a onClick={this.cancelAddress}>Cancel</a>
          <input type='submit' className='btn' value="Submit" />
        </div>
      </form>
    );
  }
}

NewAddress.propTypes = {
  customerId: React.PropTypes.number
};
