'use strict';

import React, { PropTypes } from 'react';

const SubTitle = props => {
  if (props.subtitle) {
    return (
      <span className="fc-section-title-subtitle fc-light">
        &nbsp;
        { props.subtitle }
      </span>
    );
  }
};

SubTitle.propTypes = {
  subtitle: PropTypes.node
};

const Title = props => {
  return (
    <h1 className="fc-section-title-title">
      { props.title }
      { SubTitle(props) }
    </h1>
  );
};

Title.propTypes = {
  title: PropTypes.string,
  subtitle: PropTypes.node
};

export default Title;
