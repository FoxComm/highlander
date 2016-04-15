import React, { PropTypes } from 'react';
import { Link } from '../link';

import TextFit from '../text-fit/text-fit';

//styles
import styles from '../customers/title-block.css';

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

  customerLink(text) {
    return <Link to="customer-details" params={{customerId: this.order.customer.id}}>{text}</Link>;
  }

  get customerGroups() {
    const customer = this.order.customer;

    if (customer.isGuest) {
      return <div styleName="guest">Guest</div>;
    } else if (customer.groups) {
      return (
        <div>
          {customer.groups.map((customer) => {
            return <div styleName="group">{customer}</div>;
          })}
        </div>
      );
    } else {
      return <div>None</div>;
    }
  }

  render() {
    const order = this.order;
    const customer = order.customer;
    const customerRank = customer.isGuest ? 'Guest' : customer.rank;

    let avatar = null;

    if (customer.avatarUrl) {
      avatar = <img src={customer.avatarUrl} />;
    } else {
      avatar = <i className="icon-customer"></i>;
    }

    return (
      <div className="fc-customer-info fc-content-box">
        <div styleName="header">
          <div styleName="head">
            <div styleName="rank">
              {customerRank}
            </div>
          </div>
          <div styleName="avatar">
            {avatar}
          </div>
          <div styleName="name">
            <TextFit fontSize={3} maxFontSize={3}>{this.customerLink(customer.name)}</TextFit>
          </div>
          <div styleName="email">
            <TextFit fontSize={1.7}>{this.customerLink(customer.email)}</TextFit>
          </div>
        </div>
        <article styleName="body">
          <ul styleName="fields">
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
            <li styleName="groups">
              <i className="icon-customers"></i>
              {this.ensureNotEmpty(this.customerGroups)}
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
