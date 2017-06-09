import React from 'react';
import PropTypes from 'prop-types';

const ContentTypeCode = props => {
  return <span className="fc-content-type-code">{props.value}</span>;
};

ContentTypeCode.propTypes = {
  value: PropTypes.string.isRequired
};

export default ContentTypeCode;
