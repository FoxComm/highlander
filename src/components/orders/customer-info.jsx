'use strict';

import React, { PropTypes } from 'react';

export default class CustomerInfo extends React.Component {
  render() {
    let order = this.props.order;
    let customer = order.customer;

    const isGuest = customer.isGuest;
    const customerRank = isGuest ? 'Guest' : customer.rank;

    let customerGroups = null;

    if (isGuest) {
      customerGroups = <div className="fc-customer-info-guest">Guest</div>;
    } else if (customer.groups) {
      customerGroups = (
        <div>
          {customer.groups.map((customer) => {
            return <div className="fc-customer-info-group">{customer}</div>;
          })}
        </div>
      );
    }

    function ensureNotEmpty(val) {
      return val || <span>&nbsp;</span>;
    }

    let avatar = null;

    if (customer.avatarUrl) {
      avatar = <img src={customer.avatarUrl} />;
    } else {
      avatar = <i className="icon-customer"></i>;
    }

    return (
      <div className="fc-customer-info fc-content-box">
        <div className="fc-customer-info-header">
          <div className="fc-customer-info-head">
            <div className="fc-customer-info-rank">
              {customerRank}
            </div>
          </div>
          <div className="fc-customer-info-avatar">
            {avatar}
          </div>
          <div className="fc-customer-info-name">
            {customer.firstName} {customer.lastName}
          </div>
          <div className="fc-customer-info-email">
            {customer.email}
          </div>
        </div>
        <article className="fc-customer-info-body">
          <ul className="fc-customer-info-fields">
            <li>
              <i className="icon-customer"></i>
              <div>{ensureNotEmpty(customer.id)}</div>
            </li>
            <li>
              <i className="icon-phone"></i>
              <div>{ensureNotEmpty(customer.phoneNumber)}</div>
            </li>
            <li>
              <i className="icon-location"></i>
              <div>{ensureNotEmpty(customer.location)}</div>
            </li>
            <li>
              <i className="icon-tablet"></i>
              <div>{ensureNotEmpty(customer.modality)}</div>
            </li>
            <li className="fc-customer-info-groups">
              <i className="icon-customers"></i>
              {ensureNotEmpty(customerGroups)}
            </li>
          </ul>
        </article>
      </div>
    );
  }
}

CustomerInfo.propTypes = {
  order: PropTypes.object
};
