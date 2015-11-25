import _ from 'lodash';
import * as CardUtils from '../../lib/credit-card-utils';
import GiftCardCode from '../../components/gift-cards/gift-card-code';
import React, { PropTypes } from 'react';
import Currency from '../common/currency';
import TableRow from '../table/row';
import TableCell from '../table/cell';
import CreditCardDetails from '../../components/credit-cards/card-details';
import AddressDetails from '../addresses/address-details';
import { autobind } from 'core-decorators';
import static_url from '../../lib/s3';
import { Button, EditButton } from '../common/buttons';

export default class PaymentMethodRow extends React.Component {
  constructor(props, context) {
    super(props, context);

    this.paymentMethod = this.props.paymentMethod;
    this.presenter = null;

    switch (this.paymentMethod.type) {
      case 'giftCard':
        this.presenter = new GiftCardRow(this.paymentMethod);
        break;

      case 'creditCard':
        this.presenter = new CreditCardRow(this.paymentMethod);
        break;

      case 'storeCredit':
        this.presenter = new StoreCreditRow(this.paymentMethod);
        break;
    }

    this.state = {
      showDetails: false
    };
  }

  @autobind
  toggleDetails() {
    this.setState({
      ...this.state,
      showDetails: !this.state.showDetails
    });
  }

  render() {
    let nextDetailAction = null;
    let details = null;
    let editActions = null;

    if (this.state.showDetails) {
      nextDetailAction = 'up';
      details = this.presenter.details;
    } else {
      nextDetailAction = 'down';
      details = '';
    }

    if (this.props.isEditing) {
      editActions = (
        <TableCell>
          <EditButton  />
          <Button icon='trash' className="fc-btn-remove" />
        </TableCell>
      );
    }

    return (
      <TableRow>
        <TableCell>
          <div className="fc-payment-method fc-grid">
            <div className="fc-left">
              <i className={`icon-chevron-${nextDetailAction}`} onClick={this.toggleDetails}></i>
            </div>
            <div className="fc-col-md-8-12">
              <div className="fc-left">
                <img className="fc-icon-lg" src={this.presenter.icon}></img>
              </div>
              {this.presenter.summary}
            </div>
            <div className="fc-payment-method-details">
              <div className="fc-push-md-3-12 fc-col-md-9-12">
                {details}
              </div>
            </div>
          </div>
        </TableCell>
        <TableCell>
          <div>
            <div>
              <Currency value={this.presenter.amount} />
            </div>
          </div>
        </TableCell>
        {editActions}
      </TableRow>
    );
  }
}

class GiftCardRow {
  constructor(giftCard) {
    this.giftCard = giftCard;
  }

  get amount() {
    return this.giftCard.amount;
  }

  get details() {
    const futureBalance = this.giftCard.availableBalance - this.amount;

    return (
      <div>
        <dl>
          <dt>Available Balance</dt>
          <dd><Currency value={this.giftCard.availableBalance} /></dd>
        </dl>
        <dl>
          <dt>Future Available Balance</dt>
          <dd><Currency value={futureBalance} /></dd>
        </dl>
      </div>
    );
  }

  get icon() {
    return static_url('images/payments/payment_gift_card.png');
  }

  get summary() {
    return (
      <div className="fc-left">
        <div className="fc-strong">Gift Card</div>
        <div><GiftCardCode value={this.giftCard.code} /></div>
      </div>
    );
  }
}

class StoreCreditRow {
  constructor(storeCredit) {
    this.storeCredit = storeCredit;
  }

  get amount() {
    return this.storeCredit.amount;
  }

  get details() {
    const futureBalance = this.storeCredit.availableBalance - this.amount;

    return (
      <div>
        <dl>
          <dt>Available Balance</dt>
          <dd><Currency value={this.storeCredit.availableBalance} /></dd>
        </dl>
        <dl>
          <dt>Future Available Balance</dt>
          <dd><Currency value={futureBalance} /></dd>
        </dl>
      </div>
    );
  }

  get icon() {
    return static_url('images/payments/payment_store_credit.png');
  }

  get summary() {
    return (
      <div className="fc-left">
        <div className="fc-strong">Store Credit</div>
      </div>
    );
  }
}

class CreditCardRow {
  constructor(creditCard) {
    this.creditCard = creditCard;
  }

  get amount() {
    return _.get(this.creditCard, 'amount', 0);
  }

  get icon() {
    const brand = this.creditCard.brand.toLowerCase();
    const img = `images/payments/payment_${brand}.png`;

    return static_url(img);
  }

  get details() {
    const card = this.creditCard;

    return (
      <div>
        <dl>
          <dt>Name on Card</dt>
          <dd>{card.holderName}</dd>
        </dl>
        <dl>
          <dt>Billing Address</dt>
          <AddressDetails address={card.address} />
        </dl>
      </div>
    );
  }

  get summary() {
    return (
      <div className="fc-left">
        <div className="fc-strong">{CardUtils.formatNumber(this.creditCard)}</div>
        <div>{CardUtils.formatExpiration(this.creditCard)}</div>
      </div>
    );
  }
}

PaymentMethodRow.propTypes = {
  paymentMethod: PropTypes.object.isRequired,
  isEditing: PropTypes.bool.isRequired
};
