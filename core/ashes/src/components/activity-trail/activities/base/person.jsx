
import React, { PropTypes} from 'react';

const Person = props => {
  return <a className="fc-activity__link" href={`mailto:${props.email}`}>{props.name}</a>;
};

Person.propTypes = {
  name: PropTypes.string.isRequired,
  email: PropTypes.string.isRequired,
};

export default Person;
