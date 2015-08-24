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
      states: {},
      // @TODO countries should be retrieved from server, when we'll have an endpoint for this
      countries: [
        {
          id: 'us',
          name: 'United States'
        },
        {
          id: 'other',
          name: 'Other'
        }
      ],
      formData: {
        countryId: 'us'
      }
    };
  }

  componentDidMount() {
    let formData = _.extend({}, this.state.formData);
    let dummyRegions = [
      {
        id: 1,
        name: 'Region 1'
      },
      {
        id: 2,
        name: 'Region 2'
      }
    ];

    Api.get('/states')
      .then((res) => {
        formData.stateId = res[0].id;
        this.setState({
          states: {us: res, other: dummyRegions}, // @TODO this sould be fixed when server endpoint will be ready
          formData: formData
        });
      })
      .catch((err) => {
        console.log(err);
      });
  }

  onSubmitForm(event) {
    event.preventDefault();

    Api.post(event.target.getAttribute('action'), this.state.formData)
      .then((res) => {
        AddressStore.update(res);
      })
      .catch((err) => {
        console.log(err);
      });
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

  getCountryRegions() {
    return this.state.states[this.state.formData.countryId] || [];
  }

  render() {
    return (
      <form action={`/customers/${this.props.customerId}/addresses`} className='vertical' method='POST'
            onSubmit={this.onSubmitForm.bind(this)} onChange={this.onChangeValue.bind(this)}>
        <div>
          <label htmlFor="name">Name</label>
          <input type="text" name="name" className='control' required/>
        </div>
        <div>
          <label htmlFor="street1">Address 1</label>
          <input type="text" name="street1" className='control' required/>
        </div>
        <div>
          <label htmlFor="street2">Address 2 (option)</label>
          <input type="text" name="street2" className='control'/>
        </div>
        <div>
          <label htmlFor="stateId">{ this.state.formData.countryId === 'us' ? 'State' : 'Region'}</label>
          <select name="stateId" value={this.state.formData.stateId}>
            {this.getCountryRegions().map((state, index) => {
              return <option value={state.id} key={`${index}-${state.id}`}>{state.name}</option>;
            })}
          </select>
        </div>
        <div>
          <label htmlFor="city">City</label>
          <input type="text" name="city" className='control' required/>
        </div>
        <div>
          <label htmlFor="zip">{ this.state.formData.countryId === 'us' ? 'Zip Code' : 'Postal Code'}</label>
          <input type="text" name="zip" className='control' required/>
        </div>
        <div>
          <label htmlFor="countryId">Country</label>
          <select name="countryId">
            {this.state.countries.map((country, index) => {
              return <option value={country.id} key={`${index}-${country.id}`}>{country.name}</option>;
            })}
          </select>
        </div>
        <div>
          <label htmlFor="phoneNumber">Phone</label>
          <input type="tel" name="phoneNumber" className='control' required/>
        </div>
        <div>
          <a onClick={this.cancelAddress}>Cancel</a>
          <input type='submit' className='btn' value="Submit"/>
        </div>
      </form>
    );
  }
}

NewAddress.propTypes = {
  customerId: React.PropTypes.number
};
