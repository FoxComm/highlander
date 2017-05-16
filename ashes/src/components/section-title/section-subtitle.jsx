import React from 'react';
import PropTypes from 'prop-types';

const SectionSubtitle = props => {
  return <h2>{ props.children }</h2>;
};

SectionSubtitle.propTypes = {
  children: PropTypes.node.isRequired,
};

export default SectionSubtitle;
