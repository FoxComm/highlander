
import React from 'react';
import PropTypes from 'prop-types';

import { Link } from 'components/link';

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
