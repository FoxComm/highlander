import React from 'react';
import PropTypes from 'prop-types';
import createFragment from 'react-addons-create-fragment';

const Title = props => {
  return props.tag({className: 'fc-section-title__title'}, createFragment({
    title: props.title,
    subtitle: props.subtitle && <span className="fc-section-title__subtitle fc-light">&nbsp;{props.subtitle}</span>
  }));
};

Title.propTypes = {
  title: PropTypes.node,
  subtitle: PropTypes.node,
  tag: PropTypes.oneOf([React.DOM.h1, React.DOM.h2]),
};

Title.defaultProps = {
  tag: React.DOM.h2,
};

export default Title;
