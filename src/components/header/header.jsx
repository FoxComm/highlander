'use strict';

import React from 'react';
import { inflect } from 'fleck';

export default class Header extends React.Component {
  render() {
    let { router }  = this.context;
    let pathname = router.getCurrentPathname().replace(/^\/|\/$/gm, '');
    let items = pathname.split('/').map((item, index) => {
      let classname = index > 0 ? 'icon-chevron-right' : null;
      let itemName = inflect(item, 'capitalize');
      return <span className={classname}>{` ${itemName} `}</span>;
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

Header.contextTypes = {
  router: React.PropTypes.func
};
