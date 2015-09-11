'use strict';

import React from 'react';

export default class CustomerInfo extends React.Component {
  render() {
    let retrn = this.props.return;
    let customer = retrn.customer;

    const isGuest = customer.isGuest;
    const customerRank = isGuest ? 'Guest' : customer.rank;

    let customerGroups = null;

    if (isGuest) {
      customerGroups = <div className="fc-customer-info-guest">Guest</div>;
    } else if (customer.groups) {
      customerGroups = (
        <div>
          {customer.groups.map((customer) => {
            return <div className="fc-customer-info-group">{customer}</div>
          })}
        </div>
      );
    }

    function ensureNotEmpty(val) {
      return val || <span>&nbsp;</span>;
    }

    let avatar = null;

    if (customer.avatarUrl) {
      avatar = <img src={customer.avatarUrl} />
    } else {
      avatar = <i className="fa fa-user"></i>
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
        <article>
          <ul className="fc-customer-info-fields fa-ul">
            <li>
              <i className="fa fa-li fa-user"></i>
              <div>{ensureNotEmpty(customer.id)}</div>
            </li>
            <li>
              <i className="fa fa-li fa-phone fa-flip-horizontal"></i>
              <div>{ensureNotEmpty(customer.phoneNumber)}</div>
            </li>
            <li>
              <i className="fa fa-li fa-map-marker"></i>
              <div>{ensureNotEmpty(customer.location)}</div>
            </li>
            <li>
              <i className="fa fa-li fa-mobile"></i>
              <div>{ensureNotEmpty(customer.modality)}</div>
            </li>
            <li className="fc-customer-info-groups">
              <i className="fa fa-li fa-users"></i>
              {ensureNotEmpty(customerGroups)}
            </li>
          </ul>
        </article>
      </div>
    );
  }
}

CustomerInfo.propTypes = {
  return: React.PropTypes.object
};
