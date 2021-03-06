/* @flow */

// libs
import React, { Component } from 'react';

//components
import { Link } from 'components/link';
import Icon from 'components/core/icon';

// @todo refactor this component, it almost identical to ../customers/title-block.jsx
import styles from 'components/customers/title-block.css';

type Props = {
  customer: {
    id: number,
    name: string,
    email: string,
    isGuest: boolean,
    groups: Array<string>,
    avatarUrl?: string,
    rank: number,
    phoneNumber: string,
    location: string,
  },
};

export default class CustomerInfo extends Component {
  props: Props;

  ensureNotEmpty(val: number | string) {
    return val ? <span>{val}</span> : <span>&nbsp;</span>;
  }

  customerLink(text: string) {
    const params = { customerId: this.props.customer.id };

    return <Link to="customer-details" params={params} title={text}>{text}</Link>;
  }

  get customerGroups() {
    const { customer } = this.props;

    if (customer.isGuest) {
      return <div styleName="guest">Guest</div>;
    } else if (customer.groups) {
      return (
        <div>
          {customer.groups.map(customer => {
            return <div styleName="group">{customer}</div>;
          })}
        </div>
      );
    } else {
      return <div>None</div>;
    }
  }

  render() {
    const { customer } = this.props;
    const customerRank = customer.isGuest ? 'Guest' : customer.rank;

    let avatar = null;

    if (customer.avatarUrl) {
      avatar = <img src={customer.avatarUrl} />;
    } else {
      avatar = <Icon name="customer" />;
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
            {this.customerLink(customer.name)}
          </div>
          <div styleName="email">
            {this.customerLink(customer.email)}
          </div>
        </div>
        <article styleName="body">
          <ul styleName="fields">
            <li>
              <Icon name="customer" />
              <div>{this.ensureNotEmpty(customer.id)}</div>
            </li>
            <li>
              <Icon name="phone" />
              <div>{this.ensureNotEmpty(customer.phoneNumber)}</div>
            </li>
            <li>
              <Icon name="location" />
              <div>{this.ensureNotEmpty(customer.location)}</div>
            </li>
            <li styleName="groups">
              <Icon name="customers" />
              {this.customerGroups}
            </li>
          </ul>
        </article>
      </div>
    );
  }
}
