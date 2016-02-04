
// libs
import React, { PropTypes } from 'react';
import { get } from 'sprout-data';

const OriginType = (props) => {
  console.log(props);

  const type = get(props, ['value', 'originType']);
  let formattedType = '';
  switch (type) {
    case 'csrAppeasement':
      formattedType = 'CSR Appeasement';
      break;
    case 'giftCardTransfer':
      formattedType = 'Gift Card Transfer';
      break;
    default:
      formattedType = type;
  }

  return (
    <div className="fc-origin-type">{formattedType}</div>
  );
};

OriginType.propTypes = {

};

export default OriginType;
