'use strict';

import React, {PropTypes} from 'react';
import { inflect } from 'fleck';

export default class Header extends React.Component {

  static contextTypes = {
    location: PropTypes.object
  };

  render() {
    const { location }  = this.context;
    let pathname = location.pathname.replace(/^\/|\/$/gm, '');
    let items = pathname.split('/').map((item, index) => {
      let classname = index > 0 ? 'icon-chevron-right' : null;
      let itemName = inflect(item, 'capitalize');
      return <span className={classname} key={`header-item-${index}`}>{` ${itemName} `}</span>;
    });
    let breadcrumb = <div className="breadcrumb">{items}</div>;

    return (
      <header role='banner' className="fc-header">
        {breadcrumb}
        <div className="sub-nav">
          <div className="notifications">
            <i className="icon-bell"></i>
          </div>
          <div className="sort">Name <i className="icon-chevron-down"></i></div>
        </div>
      </header>
    );
  }
}
