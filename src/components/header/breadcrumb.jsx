
// libs
import React, {PropTypes} from 'react';
import { inflect } from 'fleck';

export default class Breadcrumb extends React.Component {

  static contextTypes = {
    location: PropTypes.object
  };

  render() {
    const pathname = this.context.location.pathname.replace(/^\/|\/$/gm, '');

    const items = pathname.split('/').map((item, index) => {
      let classname = index > 0 ? 'icon-chevron-right' : null;
      let itemName = inflect(item, 'capitalize');
      return <span className={classname} key={`header-item-${index}`}>{` ${itemName} `}</span>;
    });

    return <div className="breadcrumb">{items}</div>;
  }
}
