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

export default SubTitle;
