import React, { PropTypes } from 'react';

const SectionSubtitle = props => {
  return <h2>{ props.children }</h2>;
};

SectionSubtitle.propTypes = {
  children: PropTypes.node.isRequired,
};

export default SectionSubtitle;
