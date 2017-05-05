
import React from 'react';
import PropTypes from 'prop-types';

const Person = props => {
  return <a className="fc-activity__link" href={`mailto:${props.email}`}>{props.name}</a>;
};

Person.propTypes = {
  name: PropTypes.string.isRequired,
  email: PropTypes.string.isRequired,
};

export default Person;
