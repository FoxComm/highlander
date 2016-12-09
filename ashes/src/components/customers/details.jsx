
import _ from 'lodash';
import React, { PropTypes } from 'react';
import CustomerContacts from './contacts';
import CustomerAccountPassword from './account-password';
import CustomerAddressBook from './address-book';
import CustomerCreditCards from './credit-cards';
import CustomerAccountStatus from './account-status';
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
    const customer = this.props.entity;
    const addresses = this.props.addresses;
    return (
      <div className="fc-customer-details">
        <SectionSubtitle>Details</SectionSubtitle>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-2">
            <CustomerContacts customerId={ customer.id } />
          </div>
          <div className="fc-col-md-1-2">
            <CustomerAccountPassword />
          </div>
        </div>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-1">
            <CustomerAddressBook customerId={ customer.id } addresses={ addresses } />
          </div>
        </div>
        <div className="fc-grid fc-grid-gutter">
          <div className="fc-col-md-1-1">
            <CustomerCreditCards customerId={ customer.id } addresses={ addresses } />
          </div>
        </div>
          <div className="fc-col-md-1-2">
            <CustomerAccountStatus customer={ customer }/>
          </div>
        </div>
      </div>
    );
  }
};
