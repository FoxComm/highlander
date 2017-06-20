import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import CustomerContacts from './contacts';
import CustomerAddressBook from './address-book';
import CustomerCreditCards from './credit-cards';
import CustomerAccountStatus from './account-status';
import CustomerSuggestProducts from './suggest-products';
import CustomerGroups from './groups/groups';
import SectionSubtitle from '../section-title/section-subtitle';
import { connect } from 'react-redux';
import * as AddressesActions from '../../modules/customers/addresses';

@connect((state, props) => ({
  ...state.customers.details[props.entity.id],
  addresses: _.get(state.customers.addresses, [props.entity.id, 'addresses'], [])
}), AddressesActions)
export default class CustomerDetails extends React.Component {

  static propTypes = {
    entity: PropTypes.object,
    addresses: PropTypes.array,
    fetchAddresses: PropTypes.func
  };

  componentDidMount() {
    this.props.fetchAddresses(this.props.entity.id);
  }

  render() {
    const { entity: customer, addresses } = this.props;

    return (
      <div className="fc-customer-details">
        <SectionSubtitle>Details</SectionSubtitle>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-3">
            <CustomerContacts customerId={customer.id} />
          </div>
          <div className="fc-col-md-1-3">
            <CustomerGroups customer={customer} groups={customer.groups} customerId={customer.id} />
          </div>
          <div className="fc-col-md-1-3">
            <CustomerSuggestProducts customer={customer} />
          </div>
        </div>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-1">
            <CustomerAddressBook customerId={customer.id} addresses={addresses} />
          </div>
        </div>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-1">
            <CustomerCreditCards customerId={customer.id} addresses={addresses} />
          </div>
        </div>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-2">
            <CustomerAccountStatus customer={customer} />
          </div>
        </div>
      </div>
    );
  }
}
