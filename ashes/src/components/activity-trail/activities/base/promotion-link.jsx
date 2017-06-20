
import React from 'react';
import PropTypes from 'prop-types';

import { Link } from 'components/link';

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
