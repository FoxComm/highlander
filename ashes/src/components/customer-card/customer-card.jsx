/* @flow */

import React, { Component } from 'react';

import { Link } from 'components/link';
import TextFit from 'components/text-fit/text-fit';

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

  ensureNotEmpty(val: number|string) {
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
    const { customer } = this.props;
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
            <TextFit fontSize={3} maxFontSize={3}>
              {this.customerLink(customer.name)}
            </TextFit>
          </div>
          <div styleName="email">
            <TextFit fontSize={1.7}>
              {this.customerLink(customer.email)}
            </TextFit>
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
            <li styleName="groups">
              <i className="icon-customers"></i>
              {this.customerGroups}
            </li>
          </ul>
        </article>
      </div>
    );
  }
}
