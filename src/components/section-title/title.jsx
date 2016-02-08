import React, { PropTypes } from 'react';

const Title = props => {
  return props.tag({className: 'fc-section-title__title'}, [
    props.title,
    props.subtitle && <span className="fc-section-title__subtitle fc-light">&nbsp;{props.subtitle}</span>
  ]);
};

Title.propTypes = {
  title: PropTypes.node,
  subtitle: PropTypes.node,
  tag: PropTypes.oneOf([React.DOM.h1, React.DOM.h2]).isRequired,
};

Title.defaultProps = {
  tag: React.DOM.h2,
};

export default Title;
