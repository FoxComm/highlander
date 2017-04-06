
import React, { PropTypes } from 'react';
import { Link } from '../../../link';

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
