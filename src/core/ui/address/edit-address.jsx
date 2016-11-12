// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind, debounce } from 'core-decorators';
import { connect } from 'react-redux';

// localization
import localized from 'lib/i18n';
import type { Localized } from 'lib/i18n';

// components
import { TextInput } from '../inputs';
import { FormField } from 'ui/forms';
import Autocomplete from 'ui/autocomplete';
import Checkbox from '../checkbox/checkbox';
import Loader from '../loader';

// styles
import styles from './address.css';

import type { Address } from 'types/address';
// actions
import { initAddressData } from 'modules/edit-address';

type EditShippingProps = Localized & {
  onUpdate: (address: Address) => void,
  initAddressData: (address: Address) => Promise,
}

function mapStateToProps(state) {
  return {
    countries: state.countries,
  };
}


type State = {
  address: Address|{},
  lastSyncedAddress: ?Address,
}

/* ::`*/
@connect(mapStateToProps, { initAddressData })
@localized
/* ::`*/
export default class EditAddress extends Component {
  props: EditShippingProps;
  lookupXhr: ?XMLHttpRequest;

  state: State = {
    address: {},
  };

  componentDidMount() {
    const { address } = this.props;
    this.resolveCountry(address);
  }

  componentWillReceiveProps(nextProps) {
    if (nextProps.address != this.state.lastSyncedAddress) {
      this.resolveCountry(nextProps.address);
    }
  }

  resolveCountry(address) {
    this.props.initAddressData(address).then(uiAddressData => {
      this.setState({
        address: uiAddressData,
        lastSyncedAddress: address,
      });
    });
  }

  get selectedCountry() {
    const { countries } = this.props;
    const { address } = this.state;
    const selectedCountry = _.find(countries.list, {alpha3: _.get(address.country, 'alpha3', 'USA')});
    return countries.details[selectedCountry && selectedCountry.id] || {
      regions: [],
    };
  }

  get addressState() {
    const { address } = this.state;
    return _.get(address, 'state', this.selectedCountry.regions[0]) || {};
  }

  setAddressData(key, value) {
    const newAddress = {
      ...this.state.address,
      [key]: value,
    };

    this.setState({
      address: newAddress,
    });

    this.props.onUpdate(newAddress);
  }

  @debounce(200)
  tryAutopopulateFromZip(zip) {
    // $FlowFixMe: decorators are not supported
    const selectedCountry = this.selectedCountry;

    if (zip && selectedCountry.alpha3 == 'USA') {
      if (this.lookupXhr) {
        this.lookupXhr.abort();
        this.lookupXhr = null;
      }

      this.lookupXhr = makeXhr(`/lookup-zip/usa/${zip}`).then(
        result => {
          this.setAddressData('city', result.city);
          const currentState = _.find(selectedCountry.regions, region => {
            return region.name.toLowerCase() == result.state.toLowerCase();
          });
          if (currentState) {
            this.setAddressData('state', currentState);
          }
        },
        err => {
          console.error(err);
        }
      );
    }
  }

  @autobind
  handleZipChange({target}) {
    this.setAddressData('zip', target.value);

    this.tryAutopopulateFromZip(target.value);
  }

  @autobind
  changeFormData({target}) {
    this.setAddressData(target.name, target.value);
  }

  @autobind
  changeState(item) {
    this.setAddressData('state', item);
  }

  @autobind
  changeDefault(value) {
    this.setAddressData('isDefault', value);
  }

  get isAddressLoaded(): boolean {
    const { address } = this.state;

    return !!_.get(address, 'state.name', false);
  }

  render() {
    if (!this.isAddressLoaded) return <Loader size="m"/>;

    const props: EditShippingProps = this.props;
    const { t } = props;
    const selectedCountry = this.selectedCountry;
    const data = this.state.address;

    const checked = _.get(data, 'isDefault', false);

    return (
      <div>
        <Checkbox
          name="isDefault"
          checked={checked}
          onChange={({target}) => this.changeDefault(target.checked)}
          id="set-default-address"
        >
          Make this address my default
        </Checkbox>

        <FormField styleName="text-field">
          <TextInput required
            name="name" placeholder={t('FIRST & LAST NAME')} value={data.name} onChange={this.changeFormData}
          />
        </FormField>
        <FormField styleName="text-field">
          <TextInput
            required
            name="address1" placeholder={t('STREET ADDRESS 1')} value={data.address1} onChange={this.changeFormData}
          />
        </FormField>
        <FormField styleName="text-field">
          <TextInput
            name="address2" placeholder={t('STREET ADDRESS 2 (optional)')} value={data.address2}
            onChange={this.changeFormData}
          />
        </FormField>
        <FormField styleName="text-field" validator="zipCode">
          <TextInput required placeholder={t('ZIP')} onChange={this.handleZipChange} value={data.zip} />
        </FormField>
        <FormField styleName="text-field">
          <TextInput required name="city" placeholder={t('CITY')} onChange={this.changeFormData} value={data.city}/>
        </FormField>
        <FormField styleName="text-field">
          <Autocomplete
            inputProps={{
              placeholder: t('STATE'),
            }}
            getItemValue={item => item.name}
            items={selectedCountry.regions}
            onSelect={this.changeState}
            selectedItem={this.addressState}
          />
        </FormField>
        <FormField label={t('Phone Number')} styleName="text-field" validator="phoneNumber">
          <TextInput
            required
            name="phoneNumber"
            type="tel"
            placeholder={t('PHONE')}
            onChange={this.changeFormData}
            value={data.phoneNumber}
          />
        </FormField>
      </div>
    );
  }
}
