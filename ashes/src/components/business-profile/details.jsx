/* @flow */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import _ from 'lodash';
import { getUserId } from 'lib/claims';

// components
import { Dropdown, DropdownItem } from 'components/dropdown';
import { PageTitle } from 'components/section-title';
import { PrimaryButton } from 'components/common/buttons';
import ContentBox from 'components/content-box/content-box';
import FormField from 'components/forms/formfield';
import SubNav from './sub-nav';
import WaitAnimation from 'components/common/wait-animation';

// redux
import * as businessProfileActions from 'modules/business-profile/details';

// styles
import styles from './details.css';

// types
import type { BusinessProfile } from 'paragons/business-profile';

type Props = {
  details: {
    businessProfile: ?BusinessProfile,
  },
  isUpdating: boolean,
  isUpdating: ?Object,
  updateBusinessProfile: Function,
};

const LIST_STATES = {
  Alabama: 'AL',
  Alaska: 'AK',
  'American Samoa': 'AS',
  Arizona: 'AZ',
  Arkansas: 'AR',
  California: 'CA',
  Colorado: 'CO',
  Connecticut: 'CT',
  Delaware: 'DE',
  'District Of Columbia': 'DC',
  'Federated States Of Micronesia': 'FM',
  Florida: 'FL',
  Georgia: 'GA',
  Guam: 'GU',
  Hawaii: 'HI',
  Idaho: 'ID',
  Illinois: 'IL',
  Indiana: 'IN',
  Iowa: 'IA',
  Kansas: 'KS',
  Kentucky: 'KY',
  Louisiana: 'LA',
  Maine: 'ME',
  'Marshall Islands': 'MH',
  Maryland: 'MD',
  Massachusetts: 'MA',
  Michigan: 'MI',
  Minnesota: 'MN',
  Mississippi: 'MS',
  Missouri: 'MO',
  Montana: 'MT',
  Nebraska: 'NE',
  Nevada: 'NV',
  'New Hampshire': 'NH',
  'New Jersey': 'NJ',
  'New Mexico': 'NM',
  'New York': 'NY',
  'North Carolina': 'NC',
  'North Dakota': 'ND',
  'Northern Mariana Islands': 'MP',
  Ohio: 'OH',
  Oklahoma: 'OK',
  Oregon: 'OR',
  Palau: 'PW',
  Pennsylvania: 'PA',
  'Puerto Rico': 'PR',
  'Rhode Island': 'RI',
  'South Carolina': 'SC',
  'South Dakota': 'SD',
  Tennessee: 'TN',
  Texas: 'TX',
  Utah: 'UT',
  Vermont: 'VT',
  'Virgin Islands': 'VI',
  Virginia: 'VA',
  Washington: 'WA',
  'West Virginia': 'WV',
  Wisconsin: 'WI',
  Wyoming: 'WY',
};

const mapStateToProps = (state) => {
  return {
    details: state.businessProfile.details,
    isUpdating: _.get(state.asyncActions, 'updateBusinessProfile.inProgress', false),
    updateError: _.get(state.asyncActions, 'updateBusinessProfile.err', null),
  };
};

class BusinessProfileDetails extends Component {
  props: Props;
  state: BusinessProfile = {
    legal_entity_name: '',
    bank_account_number: '',
    bank_routing_number: '',
    representative_ssn_trailing_four: '',
    legal_entity_tax_id: '',
    business_founded_day: '',
    business_founded_month: '',
    business_founded_year: '',
    address1: '',
    address2: '',
    city: '',
    state: '',
    zip: '', 
  }

  componentWillReceiveProps(nextProps: Props) {
    const { businessProfile } = nextProps.details;
    if (businessProfile) {
      this.setState({
        legal_entity_name: businessProfile.legal_entity_name,
        bank_account_number: businessProfile.bank_account_number,
        bank_routing_number: businessProfile.bank_routing_number,
        representative_ssn_trailing_four: businessProfile.representative_ssn_trailing_four,
        legal_entity_tax_id: businessProfile.legal_entity_tax_id,
        business_founded_day: businessProfile.business_founded_day,
        business_founded_month: businessProfile.business_founded_month,
        business_founded_year: businessProfile.business_founded_year,
        address1: businessProfile.address1,
        address2: businessProfile.address2,
        city: businessProfile.city,
        state: businessProfile.state,
        zip: businessProfile.zip, 
      });
    }
  }

  get foundedDates() {
    let items = [];
    for (let i = 1; i <= 31; i++) {
      items.push([i, i]);
    }
    return items;
  }

  get foundedMonths() {
    const months = ['January', 'February', 'March', 'April', 'May', 'June',
                    'July', 'August', 'September', 'October', 'November', 'December'];
    return months.map((m, i) => [i + 1, m]);
  }

  get foundedYears() {
    let items = [];
    for (let i = 2016; i >= 1900; i--) {
      items.push([i, i]);
    }
    return items;
  }

  get states() {
    return _.map(LIST_STATES, (val, key) => [val, key]);
  }

  get isDirty(): boolean {
    return !_.isEmpty(this.state.legal_entity_name) &&
      !_.isEmpty(this.state.bank_account_number) &&
      !_.isEmpty(this.state.bank_routing_number) &&
      !_.isEmpty(this.state.representative_ssn_trailing_four) &&
      !_.isEmpty(this.state.legal_entity_tax_id) &&
      !_.isEmpty(this.state.business_founded_day) &&
      !_.isEmpty(this.state.business_founded_month) &&
      !_.isEmpty(this.state.business_founded_year) &&
      !_.isEmpty(this.state.address1) &&
      !_.isEmpty(this.state.city) &&
      !_.isEmpty(this.state.state) &&
      !_.isEmpty(this.state.zip );
  }

  get renderPageTitle(): Element {
    const { isUpdating } = this.props;
    const isLoading = isUpdating;
    const disabled = isLoading || !this.isDirty;

    return (
      <PageTitle title="Update Financial Information">
        <PrimaryButton
          type="button"
          disabled={disabled}
          isLoading={isLoading}
          onClick={this.handleSubmit}>
          Update
        </PrimaryButton>
      </PageTitle>
    );
  }


  @autobind
  handleSubmit() {
    const userId = getUserId();
    const business_profile = {
      business_profile: this.state,
    };

    this.props.updateBusinessProfile(userId, business_profile);
  }

  render(): Element {
    const { businessProfile } = this.props.details;

    return (
      <div>
        {this.renderPageTitle}
        <SubNav />
        <div className="fc-grid">
          <div className="fc-col-md-2-3">
            <ContentBox styleName="content" title="Business Information">
              <div className="fc-object-form">
                <FormField
                  className="fc-object-form__field _form-field-required"
                  label="Business Name"
                  validator="ascii"
                  maxLength={255}>
                  <input
                    type="text"
                    className="fc-object-form__field-value"
                    value={this.state.legal_entity_name}
                    onChange={({target}) => this.setState({ legal_entity_name: target.value })}/>
                </FormField>
                <div styleName="founding-date">
                  <FormField
                    className="fc-object-form__field _form-field-required"
                    label="Founding Date"
                    validator="ascii"
                    maxLength={255}>
                    <Dropdown
                      name="business_founded_month"
                      placeholder="Month"
                      value={this.state.business_founded_month}
                      onChange={value => this.setState({ business_founded_month: Number(value)})}
                      items={this.foundedMonths}/>
                  </FormField>
                  <FormField
                    className="fc-object-form__field _form-field-required"
                    validator="ascii"
                    maxLength={255}>
                    <Dropdown
                      name="business_founded_day"
                      placeholder="Day"
                      value={this.state.business_founded_day}
                      onChange={value => this.setState({ business_founded_day: Number(value)})}
                      items={this.foundedDates}/>
                  </FormField>
                  <FormField
                    className="fc-object-form__field _form-field-required"
                    validator="ascii"
                    maxLength={255}>
                    <Dropdown
                      name="business_founded_year"
                      placeholder="Year"
                      value={this.state.business_founded_year}
                      onChange={value => this.setState({ business_founded_year: Number(value)})}
                      items={this.foundedYears}/>
                  </FormField>
                </div>
                <FormField
                  className="fc-object-form__field _form-field-required"
                  label="Tax ID"
                  validator="ascii"
                  maxLength={255}>
                  <input
                    type="text"
                    className="fc-object-form__field-value"
                    value={this.state.legal_entity_tax_id}
                    onChange={({target}) => this.setState({ legal_entity_tax_id: target.value })}/>
                </FormField>
              </div>
            </ContentBox>
            <ContentBox styleName="content" title="Primary Location">
              <div className="fc-object-form">
                <FormField
                  className="fc-object-form__field _form-field-required"
                  label="Street Address"
                  validator="ascii"
                  maxLength={255}>
                  <input
                    type="text"
                    className="fc-object-form__field-value"
                    value={this.state.address1}
                    onChange={({target}) => this.setState({ address1: target.value })}/>
                </FormField>
                <FormField
                  className="fc-object-form__field"
                  label="Street Address 2"
                  validator="ascii"
                  maxLength={255}>
                  <input
                    type="text"
                    className="fc-object-form__field-value"
                    value={this.state.address2}
                    onChange={({target}) => this.setState({ address2: target.value })}/>
                </FormField>
                <FormField
                  className="fc-object-form__field _form-field-required"
                  label="City"
                  validator="ascii"
                  maxLength={255}>
                  <input
                    type="text"
                    className="fc-object-form__field-value"
                    value={this.state.city}
                    onChange={({target}) => this.setState({ city: target.value })}/>
                </FormField>
                <FormField
                  className="fc-object-form__field _form-field-required"
                  label="State"
                  validator="ascii"
                  maxLength={255}>
                  <Dropdown
                    name="state"
                    placeholder="State"
                    value={this.state.state}
                    onChange={value => this.setState({ state: value})}
                    items={this.states}/>
                </FormField>
                <FormField
                  className="fc-object-form__field _form-field-required"
                  label="Zip Code"
                  validator="ascii"
                  maxLength={255}>
                  <input
                    type="text"
                    className="fc-object-form__field-value"
                    value={this.state.zip}
                    onChange={({target}) => this.setState({ zip:  target.value })}/>
                </FormField>
              </div>
            </ContentBox>
            <ContentBox styleName="content" title="Financial Information">
              <div className="fc-object-form">
                <FormField
                  className="fc-object-form__field _form-field-required"
                  label="Bank Account Number"
                  validator="ascii"
                  maxLength={255}>
                  <input
                    type="text"
                    className="fc-object-form__field-value"
                    value={this.state.bank_account_number}
                    onChange={({target}) => this.setState({ bank_account_number: target.value })}/>
                </FormField>
                <FormField
                  className="fc-object-form__field _form-field-required"
                  label="Bank Routing Number"
                  validator="ascii"
                  maxLength={255}>
                  <input
                    type="text"
                    className="fc-object-form__field-value"
                    value={this.state.bank_routing_number}
                    onChange={({target}) => this.setState({ bank_routing_number: target.value })}/>
                </FormField>
                <FormField
                  className="fc-object-form__field _form-field-required"
                  label="Primary Contact's Name"
                  validator="ascii"
                  maxLength={255}>
                  <input
                    type="text"
                    className="fc-object-form__field-value"
                    value={this.state.representative_name}
                    onChange={({target}) => this.setState({ representative_name: target.value })}/>
                </FormField>
                <FormField
                  className="fc-object-form__field _form-field-required"
                  label="Last 4 Digits of Primary Contact's SSN"
                  validator="ascii"
                  maxLength={255}>
                  <input
                    type="text"
                    className="fc-object-form__field-value"
                    value={this.state.representative_ssn_trailing_four}
                    onChange={({target}) => this.setState({ representative_ssn_trailing_four: target.value })}/>
                </FormField>
              </div>
            </ContentBox>
          </div>
        </div>
      </div>
    );
  }
}

export default connect(mapStateToProps, businessProfileActions)(BusinessProfileDetails);
