
import React, { PropTypes } from 'react';
import { Link } from '../../../link';

const SkuLink = props => {
  return (
    <Link className="fc-activity__link" to="sku" params={
      {skuCode: props.code}}>
      {props.name}
    </Link>
  );
};

SkuLink.propTypes = {
  code: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired
};

export default SkuLink;
