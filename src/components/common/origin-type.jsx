
// libs
import React, { PropTypes } from 'react';
import { get } from 'sprout-data';

const OriginType = (props) => {
  const type = get(props, ['value', 'originType']);
  let formattedType = null;
  let content = null;
  switch (type) {
    case 'csrAppeasement':
      formattedType = 'CSR Appeasement';
      content = get(props, ['value', 'orderReferenceNumber']);
      break;
    case 'giftCardTransfer':
      formattedType = 'Gift Card Transfer';
      content = get(props, ['value', 'giftCard', 'code']);
      break;
    case 'customerPurchase':
      formattedType = 'Customer Purchase';
      break;
    default:
      formattedType = type;
  }

  return (
    <div className="fc-origin-type">
      <div className="fc-origin-type__type">{formattedType}</div>
      {content && <div className="fc-origin-type__content">{content}</div>}
    </div>
  );
};

OriginType.propTypes = {
  value: PropTypes.shape({
    originType: PropTypes.string,
    orderReferenceNumber: PropTypes.string,
    giftCard: PropTypes.shape({
      code: PropTypes.string
    }),
  }),
};

export default OriginType;
