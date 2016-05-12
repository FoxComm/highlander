import React, { PropTypes } from 'react';
import GiftCardCode from '../../../components/gift-cards/gift-card-code';
import Currency from '../../common/currency';
import PaymentRow from './row';

const GiftCard = props => {
  const { amount, availableBalance, code } = props.paymentMethod;

  const orderRefNum = props.order.currentOrder.referenceNumber;
  const futureBalance = availableBalance - amount;

  const deletePayment = () => {
    props.deleteOrderGiftCardPayment(orderRefNum, code);
  };

  const details = (
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

  const params = {
    details: details,
    amount: amount,
    type: 'gift_card',
    title: 'Gift Card',
    subtitle: <GiftCardCode value={code} />,
    deleteAction: deletePayment,
    ...props,
  };

  return PaymentRow(params);
};

GiftCard.propTypes = {
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
