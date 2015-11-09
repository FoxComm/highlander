'use strict';

import React, { PropTypes } from 'react';

const subtitle = (props) => {
  if (props.subtitle) {
    return (
      <span className="fc-section-title-subtitle fc-light">
        &nbsp;
        { props.subtitle }
      </span>
    );
  }
};

subtitle.propTypes = {
  subtitle: PropTypes.node
};

const Title = (props) => {
  return (
    <h1 className="fc-section-title-title">
      { props.title }
      { subtitle(props) }
    </h1>
  );
};

Title.propTypes = {
  title: PropTypes.string,
  subtitle: PropTypes.node
};

export default Title;
