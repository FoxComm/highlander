'use strict';

import React from 'react';
import AddressStore from './store';
import { dispatch } from '../../lib/dispatcher';
import Api from '../../lib/api';
import _ from 'lodash';

export default class NewAddress extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      countries: [
        {
          id: this.defaultCountryId(),
          name: 'United States'
        },
        {
          id: 2,
          name: 'Other'
        }
      ],
      regions: [],
      formData: {
        country: this.defaultCountryId()
      }
    };
  }

  /**
   * Default country (United States) id
   */
  defaultCountryId() {
    return 1;
  }

  componentDidMount() {
    this.updateRegions();
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
      target = event.target,
      formData = _.extend({}, this.state.formData, {
        [target.name]: target.name === 'country' ? +target.value : target.value
      });

    this.setState({
      formData: formData
    }, function() {
      if (target.name === 'country') {
        this.updateRegions(target.value);
      }
    });
  }

  updateRegions(country) {
    let
      countryId = country || this.state.formData.country,
      formData = _.extend({}, this.state.formData);

    Api.get(`/countries/${countryId}`)
      .then((data) => {
        formData.region = data[0].id;
        this.setState({
          regions: data,
          formData: formData
        });
      })
      .catch((error) => {
        console.log(error);
      });
  }

  cancelAddress() {
    dispatch('cancelNewAddress');
  }

  render() {
    return (
      <form action={`/customers/${this.props.customerId}/addresses`} className='vertical' method='POST'
            onSubmit={this.onSubmitForm.bind(this)} onChange={this.onChangeValue.bind(this)}>
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
          <label htmlFor="region">{ this.state.formData.country === this.defaultCountryId() ? 'State' : 'Region'}</label>
          <select name="region" value={this.state.formData.region}>
            {this.state.regions.map((state, index) => {
              return <option value={state.id} key={`${index}-${state.id}`}>{state.name}</option>;
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
          <label htmlFor="country">Country</label>
          <select name="country" value={this.state.formData.country}>
            {this.state.countries.map((country, index) => {
              return <option value={country.id} key={`${index}-${country.id}`}>{country.name}</option>;
            })}
          </select>
        </div>
        <div>
          <label htmlFor="zip">{ this.state.formData.country === this.defaultCountryId() ? 'Zip Code' : 'Postal Code'}</label>
          <input type="text" name="zip" className='control' required/>
        </div>
        <div>
          <label htmlFor="phoneNumber">Phone</label>
          <input type="tel" name="phoneNumber" required />
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
