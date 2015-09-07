'use strict';

import React from 'react';

export default class CustomerInfo extends React.Component {
  render() {
    let order = this.props.order;
    let customer = order.customer;

    const isGuest = customer.isGuest;
    // @TODO: remove mock when api will be ready
    const customerRank = isGuest ? 'Guest' : customer.rank || 'Top 10%';

    let customerGroups = null;

    if (isGuest) {
      customerGroups = <div className="customer-info-guest">Guest</div>;
    } else {
      // @TODO: remove mock when api will be ready
      const mockList = [
        'East Coast Customers', 'Lorem Ipsum Dolor Sit Amet',
        'New York Customers', 'VIP', 'VIP Private Sale'
      ];
      const customerGroupsList = customer.groups || mockList;

      customerGroups = (
        <div>
          {customerGroupsList.map((customer) => {
            return <div className="customer-info-group">{customer}</div>
          })}
        </div>
      );
    }

    return (
      <div className="customer-info fc-content-box">
        <div className="customer-info-header">
          <div className="customer-info-head">
            <div className="customer-info-rank">
              {customerRank}
            </div>
          </div>
          <div className="customer-info-avatar">
            <img src="http://www.gravatar.com/avatar/85b84caf905342803800a673141341a4?s=84"/>
          </div>
          <div className="customer-info-name">
            {customer.firstName} {customer.lastName}
          </div>
          <div className="customer-info-email">
            {customer.email}
          </div>

        </div>
        <article>
          <ul className="customer-info-fields fa-ul">
            <li>
              <i className="fa fa-li fa-user"></i>
              <div>{customer.id}</div>
            </li>
            <li>
              <i className="fa fa-li fa-phone fa-flip-horizontal"></i>
              <div>{customer.phoneNumber}</div>
            </li>
            <li>
              <i className="fa fa-li fa-map-marker"></i>
              <div>{customer.location}</div>
            </li>
            <li>
              <i className="fa fa-li fa-mobile"></i>
              <div>{customer.modality}</div>
            </li>
            <li className="customer-info-groups">
              <i className="fa fa-li fa-users"></i>
              {customerGroups}
            </li>
          </ul>
        </article>
      </div>
    );
  }
}

CustomerInfo.propTypes = {
  order: React.PropTypes.object
};
