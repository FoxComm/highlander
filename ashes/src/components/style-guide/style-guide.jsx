import React from 'react';
import PropTypes from 'prop-types';

import { IndexLink, Link } from 'components/link';

const StyleGuide = props => {
  return (
    <div className='fc-style-guide fc-list-page'>
      <div className="fc-list-page-header">
        <ul>
          <li><IndexLink to="style-guide-grid">Grid</IndexLink></li>
          <li><Link to="style-guide-buttons">Buttons</Link></li>
          <li><Link to="style-guide-containers">Containers</Link></li>
        </ul>
      </div>
      <div className="fc-list-page-content">
        {props.children}
      </div>
    </div>
  );
};

StyleGuide.propTypes = {
  children: PropTypes.node
};

export default StyleGuide;
