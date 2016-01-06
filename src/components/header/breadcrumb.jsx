
// libs
import _ from 'lodash';
import React, {PropTypes} from 'react';
import { Router } from 'react-router';
import { inflect } from 'fleck';
import { assoc } from 'sprout-data';

// components
import { Link } from '../link/index';

export default class Breadcrumb extends React.Component {

  static contextTypes = {
    location: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired
  };

  constructor(props, context) {
    super(props, context);
    console.log('constructor');
    console.log(context);
  }

  // static contextTypes = {
  //   location: PropTypes.object.isRequired,
  //   routes: PropTypes.array.isRequired,
  //   params: PropTypes.array.isRequired,
  //   router: PropTypes.func.isRequired
  // };

  render() {
    const pathname = this.context.location.pathname.replace(/^\/|\/$/gm, '');

    console.log(this.context.location);
    console.log(this.context.routes);
    console.log(this.context.params);
    console.log(this.context.router);
    if (this.context.router) {
      console.log(this.context.router.getCurrentPath());
      console.log(this.context.router.getCurrentParams());
    }
    console.log(pathname);

    const pathParts = pathname.split('/');
    // const items = pathParts.map((item, index) => {
    //   let classname = index > 0 ? 'icon-chevron-right' : null;
    //   let itemName = inflect(item, 'capitalize');
    //   console.log(item);
    //   return <span className={classname} key={`header-item-${index}`}>{` ${itemName} `}</span>;
    // });

    //ToDo:
    // - create routes for each part
    // - create details route for each `number` section (won't work with refNum and so on)
    // - detect ids properly
    // - collect route parts into clickable breadcrumbs
    // - check routes (the spec is needed, not all routes can be generated simply right now)
    // - there should be a way to determine if it is IndexLink or Link

    const acc = {routes: [], lastRoute: null, lastParams: {}};
    const pathNames = pathParts.reduce((acc, part) => {
      if (part != undefined && isNaN(part)) {
        const newRoute = _.isEmpty(acc.lastRoute) ? part : `${acc.lastRoute}-${part}`;
        const itemName = inflect(part, 'capitalize');
        const params = acc.lastParams;
        acc.routes.push(<Link to={newRoute} key={`header-item-${itemName}`} params={params}>{itemName}</Link>);
        acc.lastRoute = newRoute;
      } else {
        const newRoute = acc.lastRoute != undefined ? acc.lastRoute.substring(0, acc.lastRoute.length - 1) : '';
        const itemName = part;
        let params = assoc(acc.lastParams, 'customerId', part);
        acc.routes.push(<Link to={newRoute} key={`header-item-${itemName}`} params={params}>{itemName}</Link>);
        acc.lastRoute = newRoute;
        acc.lastParams = params;
      }
      return acc;
    }, acc);

    return <div className="breadcrumb">{pathNames.routes}</div>;
  }
}
