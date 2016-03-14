
import _ from 'lodash';
import React, { Component } from 'react';
import cssModules from 'react-css-modules';
import styles from './checkout.css';
import { autobind, debounce } from 'core-decorators';
import { connect } from 'react-redux';

import Button from 'ui/buttons';
import { TextInput } from 'ui/inputs';
import EditableBlock from 'ui/editable-block';
import { FormField, Form } from 'ui/forms';
import Autocomplete from 'ui/autocomplete';

import * as checkoutActions from 'modules/checkout';

let ViewShipping = (props: Object) => {
  return (
    <ul>
      <li><strong>{props.name}</strong></li>
      <li>{props.address1}</li>
      {props.address2 && <li>{props.address2}</li>}
      <li>{props.city}, {props.state.name} {props.zip}</li>
      <li>{props.country.name}</li>
      {props.phone && <li>{props.phone}</li>}
    </ul>
  );
};
ViewShipping = connect(state => (state.checkout.shippingData))(ViewShipping);

type ShippingProps = {
  isEditing: boolean;
  collapsed: boolean;
  editAction: Function;
}

type EditShippingProps = {
  continueAction?: Function;
  setShippingData: Function;
  selectedCountry: Object;
  state: Object;
}

function mapStateToProps(state) {
  const { shippingData } = state.checkout;

  const countries = state.countries.list;
  const selectedCountry = _.find(countries, {alpha3: _.get(shippingData.country, 'alpha3', 'USA')});
  const countryDetails = state.countries.details[selectedCountry && selectedCountry.id] || {
    regions: [],
  };

  return {
    countries: state.countries.list,
    selectedCountry: countryDetails,
    state: _.get(shippingData, 'state', countryDetails.regions[0]) || {},
    data: shippingData,
  };
}

/* ::`*/
@connect(mapStateToProps, checkoutActions)
@cssModules(styles)
/* ::`*/
class EditShipping extends Component {
  props: EditShippingProps;
  lookupXhr: ?XMLHttpRequest;

  @debounce(200)
  tryAutopopulateFromZip(zip) {
    // $FlowFixMe: decorators are not supported
    const { selectedCountry, setShippingData } = this.props;

    if (zip && selectedCountry.alpha3 == 'USA') {
      if (this.lookupXhr) {
        this.lookupXhr.abort();
        this.lookupXhr = null;
      }

      this.lookupXhr = makeXhr(`/lookup-zip/usa/${zip}`).then(
        result => {
          setShippingData('city', result.city);
          const currentState = _.find(selectedCountry.regions, region => {
            return region.name.toLowerCase() == result.state.toLowerCase();
          });
          if (currentState) {
            setShippingData('state', currentState);
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
    this.props.setShippingData('zip', target.value);

    this.tryAutopopulateFromZip(target.value);
  }

  @autobind
  changeFormData({target}) {
    this.props.setShippingData(target.name, target.value);
  }

  @autobind
  changeCountry(item) {
    this.props.setShippingData('country', item);
  }

  @autobind
  changeState(item) {
    this.props.setShippingData('state', item);
  }

  @autobind
  handleSubmit() {
    this.changeCountry(this.props.selectedCountry);
    this.changeState(this.props.state);

    this.props.continueAction();
  }

  render() {
    const props: EditShippingProps = this.props;
    const { countries, selectedCountry, data } = props;

    return (
      <Form onSubmit={this.handleSubmit} styleName="checkout-form">
        <FormField styleName="checkout-field">
          <TextInput required
            name="name" placeholder="FIRST & LAST NAME" value={data.name} onChange={this.changeFormData}
          />
        </FormField>
        <FormField styleName="checkout-field">
          <TextInput
            required
            name="address1" placeholder="STREET ADDRESS 1" value={data.address1} onChange={this.changeFormData}
          />
        </FormField>
        <FormField styleName="checkout-field">
          <TextInput
            name="address2" placeholder="STREET ADDRESS 2 (optional)" value={data.address2}
            onChange={this.changeFormData}
          />
        </FormField>
        <div styleName="union-fields">
          <FormField styleName="checkout-field">
            <Autocomplete
              inputProps={{
                placeholder: 'COUNTRY',
              }}
              getItemValue={item => item.name}
              items={countries}
              onSelect={this.changeCountry}
              selectedItem={selectedCountry}
            />
          </FormField>
          <FormField styleName="checkout-field" validator="zipCode">
            <TextInput required placeholder="ZIP" onChange={this.handleZipChange} value={data.zip} />
          </FormField>
        </div>
        <div styleName="union-fields">
          <FormField styleName="checkout-field">
            <TextInput required name="city" placeholder="CITY" onChange={this.changeFormData} value={data.city} />
          </FormField>
          <FormField styleName="checkout-field">
            <Autocomplete
              inputProps={{
                placeholder: 'STATE',
              }}
              getItemValue={item => item.name}
              items={selectedCountry.regions}
              onSelect={this.changeState}
              selectedItem={props.state}
            />
          </FormField>
        </div>
        <FormField label="Phone Number" styleName="checkout-field" validator="phoneNumber">
          <TextInput required
            name="phone" type="tel" placeholder="PHONE" onChange={this.changeFormData} value={data.phone}
          />
        </FormField>
        <Button styleName="checkout-submit" type="submit">CONTINUE</Button>
      </Form>
    );
  }
}


const Shipping = (props: ShippingProps) => {
  return (
    <EditableBlock
      styleName="checkout-block"
      title="SHIPPING"
      isEditing={props.isEditing}
      collapsed={props.collapsed}
      editAction={props.editAction}
      viewContent={<ViewShipping />}
      editContent={<EditShipping {...props} />}
    />
  );
};

export default cssModules(Shipping, styles);
