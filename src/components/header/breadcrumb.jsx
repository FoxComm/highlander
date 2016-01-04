
// libs
import _ from 'lodash';
import React, {PropTypes} from 'react';
import { inflect } from 'fleck';

export default class Breadcrumb extends React.Component {

  static contextTypes = {
    location: PropTypes.object
  };

  render() {
    const pathname = this.context.location.pathname.replace(/^\/|\/$/gm, '');

    console.log(pathname);

    const pathParts = pathname.split('/');
    const items = pathParts.map((item, index) => {
      let classname = index > 0 ? 'icon-chevron-right' : null;
      let itemName = inflect(item, 'capitalize');
      console.log(item);
      return <span className={classname} key={`header-item-${index}`}>{` ${itemName} `}</span>;
    });

    //ToDo:
    // - create routes for each part
    // - create details route for each `number` section (won't work with refNum and so on)
    // - detect ids properly
    // - collect route parts into clickable breadcrumbs
    // - check routes (the spec is needed, not all routes can be generated simply right now)
    // - there should be a way to determine if it is IndexLink or Link

    const acc = {routes: [], lastRoute: null};
    const pathNames = pathParts.reduce((acc, part) => {
      if (part != undefined && isNaN(part)) {
        const newRoute = _.isEmpty(acc.lastRoute) ? part : `${acc.lastRoute}-${part}`;
        acc.routes.push(newRoute);
        acc.lastRoute = newRoute;
      } else {
        console.log(part + ' is number!');
      }
      return acc;
    }, acc);
    console.log(pathNames);

    return <div className="breadcrumb">{items}</div>;
  }
}
