import React, { PropTypes } from 'react';
import SubTitle from './subtitle';

const Title = props => {
  const titleClass = props.isPrimary ? 'fc-section-title-primary' : 'fc-section-title-secondary';
  return (
    <h1 className={ titleClass } >
      { props.title }
      { SubTitle(props) }
    </h1>
  );
};

Title.propTypes = {
  title: PropTypes.string,
  subtitle: PropTypes.node,
  isPrimary: PropTypes.bool
};

export default Title;
