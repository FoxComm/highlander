'use strict';

import _ from 'lodash';
import React from 'react';
import { listenTo, stopListeningTo, dispatch } from '../../lib/dispatcher';
import CountryStore from '../../stores/countries';
import AddressStore from '../../stores/addresses';
import OrderStore from '../../stores/orders'
import {idGenerator} from '../../lib/forms';

const DEFAULT_COUNTRY = 'US';

export default class AddressForm extends React.Component {

  constructor(props) {
    super(props);

    let address = props.address;
    let formData = address ? _.omit(address, 'region') : {};
    formData.regionId = address ? address.region.id : null;

    this.state = {
      formData,
      countryId: address ? address.region.countryId : null
    };
  }

  get isAddingForm() {
    return !this.props.address;
  }

  componentDidMount() {
    CountryStore.listenToEvent('change', this);
    CountryStore.fetch();
  }

  componentWillUnmount() {
    CountryStore.stopListeningToEvent('change', this);
  }

  onChangeCountryStore(countries) {
    this.setState({countries});

    if (!this.state.country && !this.countryWillFetch) {
      this.updateRegions();
    }
  }

  updateRegions(countryId=this.state.countryId) {
    if (countryId === null) {
      let country = CountryStore.findWhere({alpha2: DEFAULT_COUNTRY});
      countryId = country.id;
    }

    this.countryWillFetch = true;
    CountryStore.fetch(countryId)
      .then(country => {
        this.setState({
          country,
          countryId,
          formData: _.extend(this.state.formData, {regionId: country.regions[0].id})
        });
      })
      .catch(({errors}) => {
        this.setState({errors});
      });
  }

  close() {
    dispatch('toggleModal', null);
  }

  componentDidUpdate() {
    if (this.state.errors) {
      React.findDOMNode(this.refs.errorMessages).scrollIntoView();
    }
  }

  onSubmitForm(event) {
    event.preventDefault();

    const customerId = this.props.customerId;
    const formData = this.state.formData;

    let willSaved;

    if (this.isAddingForm) {
      willSaved = AddressStore.create(customerId, formData);
    } else {
      willSaved = AddressStore.patch(customerId, this.props.address.id, formData);
    }

    willSaved
      .then((address) => {
        this.setState({errors: null});

        if (this.props.onSaved) {
          this.props.onSaved(address.id);
        }
      })
      .then(() => {
        this.close();
      })
      .catch(({errors}) => {
        this.setState({errors});
      });
  }

  onChangeValue({target}) {
    if (target.name === 'country') {
      let countryId = Number(target.value);
      this.setState({countryId});
      this.updateRegions(countryId);
    } else {
      let value = target.dataset.type === 'int' ? Number(target.value) : target.value;

      let formData = _.extend({}, this.state.formData, {
        [target.name]: value
      });

      this.setState({formData});
    }
  }

  render() {
    const state = this.state;
    const formData = state.formData;
    const countries = state.countries || [];

    const nextId = idGenerator('address-form-');

    const countryCode = state.country && state.country.alpha2;
    const regions = state.country && state.country.regions || [];
    const title = this.isAddingForm ? 'Add Address' : 'Edit Address';

    let messages = null;

    if (this.state.errors) {
      messages = (
        <div className="messages" ref="errorMessages">
          {this.state.errors.map((error, index) => {
            return <div className="fc-error"><i className="fa fa-times-circle-o"></i>{error}</div>
          })}
        </div>
      )
    }

    return (
      <div className="fc-content-box fc-address-form">
        <header className="header">
          <div className="fc-address-form-header">Address Book</div>
          <i onClick={this.close.bind(this)} className="icon-close" title="Close"></i>
        </header>
        {messages}
        <article>
          <form action={AddressStore.uri(this.props.customerId)}
                onSubmit={this.onSubmitForm.bind(this)}
                onChange={this.onChangeValue.bind(this)}>
            <ul className="fc-address-form-fields">
              <li>
                <div className="fc-address-form-field-title">{title}</div>
              </li>
              <li>
                <label htmlFor={nextId()}>Name</label>
                <input id={nextId()} name="name" type="text" value={formData.name} required />
              </li>
              <li>
                <label htmlFor={nextId()}>Country</label>
                <select name="country" id={nextId()} value={this.state.countryId}>
                  {countries.map((country, index) => {
                    return <option value={country.id} key={`${index}-${country.id}`}>{country.name}</option>;
                  })}
                </select>
              </li>
              <li>
                <label htmlFor={nextId()}>Street Address</label>
                <input id={nextId()} name="address1" type="text" value={formData.address1} required />
              </li>
              <li>
                <label htmlFor={nextId()}>
                  Street Address 2
                  &nbsp;
                  <span className="fc-address-form-comment">(optional)</span>
                </label>
                <input id={nextId()} name="address2" type="text" value={formData.address2} />
              </li>
              <li>
                <label htmlFor={nextId()}>City</label>
                <input id={nextId()} name="city" type="text" value={formData.city} required />
              </li>
              <li>
                <label htmlFor={nextId()}>
                  {CountryStore.regionName(countryCode)}
                </label>
                <select id={nextId()} name="regionId" value={formData.regionId} data-type="int" required>
                  {regions.map((state, index) => {
                    return <option value={state.id} key={`${index}-${state.id}`}>{state.name}</option>;
                  })}
                </select>
              </li>
              <li>
                <label htmlFor={nextId()}>
                  {CountryStore.zipName(countryCode)}
                </label>
                <input id={nextId()} type="text" name="zip"
                       placeholder={CountryStore.zipExample(countryCode)}
                       value={formData.zip} className='control' required />
              </li>
              <li>
                <label htmlFor={nextId()}>Phone Number</label>
                <input id={nextId()} type="tel" name="phoneNumber"
                       placeholder={CountryStore.phoneExample(countryCode)} />
              </li>
              <li className="fc-address-form-controls">
                <a onClick={this.close.bind(this)} className="fc-btn-link" href="javascript:void(0)">Cancel</a>
                <button className="fc-btn-primary" type="submit">Save and choose</button>
              </li>
            </ul>
            </form>
        </article>
      </div>
    )
  }
}

AddressForm.propTypes = {
  address: React.PropTypes.object,
  customerId: React.PropTypes.number,
  onSaved: React.PropTypes.func
};

