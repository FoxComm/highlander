import React, {PropTypes} from 'react';
import { inflect } from 'fleck';

export default class Header extends React.Component {

  static contextTypes = {
    location: PropTypes.object
  };

  get breadcrumb() {
    const pathname = this.context.location.pathname.replace(/^\/|\/$/gm, '');

    const items = pathname.split('/').map((item, index) => {
      let classname = index > 0 ? 'icon-chevron-right' : null;
      let itemName = inflect(item, 'capitalize');
      return <span className={classname} key={`header-item-${index}`}>{` ${itemName} `}</span>;
    });

    return <div className="breadcrumb">{items}</div>;
  }

  render() {
    return (
      <header role='banner' className="fc-header">
        {this.breadcrumb}
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
