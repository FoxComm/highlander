import * as CardUtils from '../../../lib/credit-card-utils';
import React, { PropTypes } from 'react';
import CreditCardDetails from '../../../components/credit-cards/card-details';
import AddressDetails from '../../addresses/address-details';
import static_url from '../../../lib/s3';
import { Button, EditButton } from '../../common/buttons';
import Row from './row';

const CreditCard = props => {
  const card = props.paymentMethod;
  const brand = card.brand.toLowerCase();
  const icon = static_url(`images/payments/payment_${brand}.png`);

  const deletePayment = () => {
    props.deleteOrderCreditCardPayment(props.order.currentOrder.refNum);
  };

  const editAction = (
    <div>
      <EditButton onClick={() => console.log("not implemented")} />
      <Button icon="trash" className="fc-btn-remove" onClick={deletePayment} />
    </div>
  );

  const details = () => {
    return(
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
  };

  const summary = (
    <div className="fc-left">
      <div className="fc-strong">{CardUtils.formatNumber(card)}</div>
      <div>{CardUtils.formatExpiration(card)}</div>
    </div>
  );

  const params = {
    details: details,
    amount: null,
    icon: icon,
    summary: summary,
    editAction: editAction,
    isEditing: props.isEditing
  };

  return <Row {...params} />;
};

CreditCard.propTypes = {
  paymentMethod: PropTypes.shape({
    paymentMethod: PropTypes.shape({
      brand: PropTypes.string.isRequired,
      holderName: PropTypes.string.isRequired,
      address: PropTypes.object.isRequired,
      expMonth: PropTypes.number.isRequired,
      expYear: PropTypes.number.isRequired,
      lastFour: PropTypes.string.isRequired
    })
  }),
  order: PropTypes.shape({
    currentOrder: PropTypes.shape({
      referenceNumber: PropTypes.string.isRequired
    })
  }),
  isEditing: PropTypes.bool.isRequired,
  deleteOrderCreditCardPayment: PropTypes.func.isRequired
};

export default CreditCard;
