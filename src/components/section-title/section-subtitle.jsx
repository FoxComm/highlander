'use strict';

import React, { PropTypes } from 'react';

const SectionSubtitle = (props) => {
  return (
    <div>
      <h2>{ props.title }</h2>
    </div>
  );
};

SectionSubtitle.propTypes = {
  title: PropTypes.string
};

export default SectionSubtitle;
