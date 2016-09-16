
import React, { PropTypes } from 'react';
import { Link } from '../../../link';

const PomotionLink = props => {
  return (
    <Link className="fc-activity__link" to="promotion" params={
      {promotionId: props.id}}>
      {props.name}
    </Link>
  );
};

PomotionLink.propTypes = {
  id: PropTypes.number.isRequired,
  name: PropTypes.string.isRequired
};

export default PomotionLink;
