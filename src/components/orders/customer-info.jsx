import React, { PropTypes } from 'react';

export default class CustomerInfo extends React.Component {
  ensureNotEmpty(val) {
    return val || <span>&nbsp;</span>;
  }

  get order() {
    return this.props.order;
  }

  get modality() {
    if (!this.order.isCart) {
      return (
        <li>
          <i className="icon-tablet"></i>
          <div>{this.ensureNotEmpty(this.order.customer.modality)}</div>
        </li>
      );
    }
  }

  render() {
    const order = this.order;
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
            {customer.name}
          </div>
          <div className="fc-customer-info-email">
            {customer.email}
          </div>
        </div>
        <article className="fc-customer-info-body">
          <ul className="fc-customer-info-fields">
            <li>
              <i className="icon-customer"></i>
              <div>{this.ensureNotEmpty(customer.id)}</div>
            </li>
            <li>
              <i className="icon-phone"></i>
              <div>{this.ensureNotEmpty(customer.phoneNumber)}</div>
            </li>
            <li>
              <i className="icon-location"></i>
              <div>{this.ensureNotEmpty(customer.location)}</div>
            </li>
            {this.modality}
            <li className="fc-customer-info-groups">
              <i className="icon-customers"></i>
              {this.ensureNotEmpty(customerGroups)}
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
