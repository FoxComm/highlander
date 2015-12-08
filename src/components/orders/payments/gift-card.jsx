import React, { PropTypes } from 'react';
import GiftCardCode from '../../../components/gift-cards/gift-card-code';
import Currency from '../../common/currency';
import static_url from '../../../lib/s3';
import { Button, EditButton } from '../../common/buttons';
import Row from './row';

const GiftCard = props => {
  const { amount, availableBalance, code } = props.paymentMethod;

  const orderRefNum = props.order.currentOrder.referenceNumber;
  const futureBalance = availableBalance - amount;
  const icon = static_url('images/payments/payment_gift_card.png');

  const deletePayment = () => {
    props.deleteOrderGiftCardPayment(orderRefNum, code);
  };

  const editAction = (
    <div>
      <EditButton onClick={() => console.log("not implemented")} />
      <Button icon="trash" className="fc-btn-remove" onClick={deletePayment} />
    </div>
  );

  const details = () => {
    return (
      <div>
        <dl>
          <dt>Available Balance</dt>
          <dd><Currency value={availableBalance} /></dd>
        </dl>
        <dl>
          <dt>Future Available Balance</dt>
          <dd><Currency value={futureBalance} /></dd>
        </dl>
      </div>
    );
  };

  const summary = (
    <div className="fc-left">
      <div className="fc-strong">Gift Card</div>
      <div><GiftCardCode value={code} /></div>
    </div>
  );

  const params = {
    details: details,
    amount: amount,
    icon: icon,
    summary: summary,
    editAction: editAction,
    isEditing: props.isEditing
  };

  return <Row {...params} />;
};

GiftCardCode.propTypes = {
  paymentMethod: PropTypes.shape({
    paymentMethod: PropTypes.shape({
      amount: PropTypes.number.isRequired,
      availableBalance: PropTypes.number.isRequired,
      code: PropTypes.string.isRequired
    })
  }),
  order: PropTypes.shape({
    currentOrder: PropTypes.shape({
      referenceNumber: PropTypes.string.isRequired
    })
  }),
  isEditing: PropTypes.bool.isRequired,
  deleteOrderGiftCardPayment: PropTypes.func.isRequired
};

export default GiftCard;
