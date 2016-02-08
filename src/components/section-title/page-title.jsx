
import React, { PropTypes } from 'react';
import SectionTitle from './section-title';

/**
 * PageTitle is formally SectionTitle for defined parent context - big page headers.
 */
const PageTitle = props => {
  return (
    <SectionTitle {...props} titleTag={ React.DOM.h1 } className="_page-context">
      {props.children}
    </SectionTitle>
  );
};

PageTitle.propTypes = {
  ...SectionTitle.propTypes,
  pageContext: PropTypes.bool,
  children: PropTypes.node,
};


export default PageTitle;
