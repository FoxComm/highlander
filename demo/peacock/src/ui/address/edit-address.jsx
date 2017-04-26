import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { autobind, debounce } from 'core-decorators';
import { connect } from 'react-redux';
import { createNumberMask } from 'lib/i18n/field-masks';
import { env } from 'lib/env';

// localization
import localized, { phoneMask } from 'lib/i18n';
import type { Localized } from 'lib/i18n';

// components
import MaskedInput from 'react-text-mask';
import { TextInput } from 'ui/text-input';
import { FormField } from 'ui/forms';
import Select from 'ui/select/select';
import Autocomplete from 'ui/autocomplete';
import Checkbox from '../checkbox/checkbox';
import Loader from '../loader';

// actions
import { initAddressData } from 'modules/edit-address';

import type { Address } from 'types/address';

import styles from './address.css';

type EditAddressProps = Localized & {
  onUpdate: (address: Address) => void,
  initAddressData: (address: Address) => Promise<*>,
  colorTheme?: string,
  withCountry?: boolean,
  withoutDefaultCheckbox?: boolean,
  title?: string,
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
  props: EditAddressProps;
  lookupXhr: ?XMLHttpRequest;

  static defaultProps = {
    colorTheme: 'default',
    withCountry: false,
    withoutDefaultCheckbox: false,
    title: '',
  };

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
    this.props.initAddressData(address).then((uiAddressData) => {
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

  get countryCode() {
    const { address } = this.state;
    return _.get(address.country, 'alpha3', 'USA');
  }

  @autobind
  handlePhoneChange(value) {
    this.setAddressData('phoneNumber', value);
  }

  get countryInput() {
    const { countries } = this.props;

    return (
      <Autocomplete
        inputProps={{
          placeholder: 'Country',
          name: 'country',
        }}
        getItemValue={item => item.name}
        items={countries.list}
        onSelect={this.changeCountry}
        selectedItem={this.selectedCountry}
      />
    );
  }

  get defaultCheckboxInput() {
    const { withoutDefaultCheckbox } = this.props;

    if (withoutDefaultCheckbox) {
      return null;
    }

    const checked = _.get(this.state.address, 'isDefault', false);

    return (
      <div styleName="checkbox">
        <Checkbox
          name="isDefault"
          checked={checked}
          onChange={({target}) => this.changeDefault(target.checked)}
          id="set-default-address"
        >
          Set as default
        </Checkbox>
      </div>
    );
  }

  get title() {
    const { title } = this.props;

    if (title != null) {
      return <div styleName="address-title">{title}</div>;
    }
  }

  get phoneInput() {
    const { address } = this.state;
    const { t } = this.props;

    const inputAttributes = {
      type: 'tel',
      name: 'phoneNumber',
      placeholder: t('Phone'),
      value: address.phoneNumber,
      required: true,
    };

    let input;

    if (this.countryCode === 'USA') {
      const onChange = ({ target: { value }}) => this.handlePhoneChange(value);
      const mask = createNumberMask(phoneMask());

      input = (
        <TextInput {...inputAttributes} styleName="text-input" pos="bottom">
          <MaskedInput
            onChange={onChange}
            mask={mask}
            placeholderChar={'\u2000'}
          />
        </TextInput>
      );
    } else {
      const onChange = ({ target: { value }}) => this.handlePhoneChange(value);
      input = (
        <TextInput {...inputAttributes} onChange={onChange} maxLength="15" />
      );
    }

    return input;
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

      this.lookupXhr = makeXhr(`${env.URL_PREFIX}/node/lookup-zip/usa/${zip}`).then(
        (result) => {
          this.setAddressData('city', result.city);
          const currentState = _.find(selectedCountry.regions, (region) => {
            return region.name.toLowerCase() == result.state.toLowerCase();
          });
          if (currentState) {
            this.setAddressData('state', currentState);
          }
        },
        (err) => {
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
  changeCountry(item) {
    this.setAddressData('country', item);
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
    if (!this.isAddressLoaded) return <Loader size="m" />;

    const props: EditAddressProps = this.props;
    const { t } = props;
    const selectedCountry = this.selectedCountry;
    const data = this.state.address;

    return (
      <div styleName={`theme-${props.colorTheme}`}>
        { this.title }
        <div styleName="form-group">
          <FormField styleName="text-field">
            <TextInput
              pos="top"
              required
              name="name"
              placeholder={t('First & Last Name')}
              value={data.name}
              onChange={this.changeFormData}
            />
          </FormField>
          <FormField
            label={t('Phone Number')}
            styleName="text-field"
            validator="phoneNumber"
          >
            {this.phoneInput}
          </FormField>
        </div>
        <div styleName="form-group">
          <FormField styleName="text-field">
            <TextInput
              pos="top"
              required
              name="address1"
              placeholder={t('Address Line 1')}
              value={data.address1}
              onChange={this.changeFormData}
            />
          </FormField>
          <FormField styleName="text-field">
            <TextInput
              pos="bottom"
              name="address2"
              placeholder={t('Address Line 2 (optional)')}
              value={data.address2}
              onChange={this.changeFormData}
            />
          </FormField>
        </div>
        <div styleName="form-group">
          <FormField styleName="text-field">
            <TextInput
              pos="top"
              required
              name="city"
              placeholder={t('City')}
              onChange={this.changeFormData} value={data.city}
            />
          </FormField>
          <div styleName="input-group">
            <FormField styleName="text-field-state">
              <Select
                inputProps={{
                  placeholder: t('State'),
                }}
                getItemValue={item => item.name}
                items={selectedCountry.regions}
                onSelect={this.changeState}
                selectedItem={this.addressState}
                name="state"
              />
            </FormField>
            <FormField
              styleName="text-field-zip"
              validator="zipCode"
            >
              <TextInput
                pos="bottom-right"
                required
                placeholder={t('Zip')}
                onChange={this.handleZipChange}
                value={data.zip}
              />
            </FormField>
          </div>
        </div>
        { this.defaultCheckboxInput }
      </div>
    );
  }
}
