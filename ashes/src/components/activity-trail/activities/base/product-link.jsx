
import React from 'react';
import PropTypes from 'prop-types';

import { Link } from 'components/link';

const ProductLink = props => {
  return (
    <Link className="fc-activity__link" to="product" params={
      {context: props.context, productId: props.id}}>
      {props.name}
    </Link>
  );
};

ProductLink.propTypes = {
  context: PropTypes.string.isRequired,
  id: PropTypes.number.isRequired,
  name: PropTypes.string.isRequired
};

export default ProductLink;
