'use strict';

import React, { PropTypes } from 'react';

const Title = (props) => {
  let subtitle = null;

  if (props.subtitle) {
    subtitle = (
      <span className="fc-section-title-subtitle fc-light">
        &nbsp;
        { props.subtitle }
      </span>
    );
  }

  return (
    <h1 className="fc-section-title-title">
      { props.title }
      { subtitle }
    </h1>
  );
};

Title.propTypes = {
  title: PropTypes.string,
  subtitle: PropTypes.node
};

export default Title;
