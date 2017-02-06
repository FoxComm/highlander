
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { get } from 'sprout-data';

// data
import { types, typeTitles } from '../../paragons/gift-card';


const OriginType = (props) => {
  const type = get(props, ['value', 'originType']);
  let formattedType = null;
  let content = null;
  switch (type) {
    case types.csrAppeasement:
      formattedType = typeTitles[type];
      content = get(props, ['value', 'cordReferenceNumber']);
      break;
    case 'giftCardTransfer':
      formattedType = typeTitles[type];
      content = get(props, ['value', 'giftCard', 'code']);
      break;
    case 'customerPurchase':
      formattedType = typeTitles[type];
      break;
    case 'custom':
      formattedType = get(props, ['value', 'metadata', 'title'], _.capitalize(type));
      break;
    default:
      formattedType = _.capitalize(type);
  }

  return (
    <div className="fc-origin-type">
      <div className="fc-origin-type__type gift-card-type">{formattedType}</div>
      {content && <div className="fc-origin-type__content">{content}</div>}
    </div>
  );
};

OriginType.propTypes = {
  value: PropTypes.shape({
    originType: PropTypes.string,
    cordReferenceNumber: PropTypes.string,
    giftCard: PropTypes.shape({
      code: PropTypes.string
    }),
  }),
};

export default OriginType;
