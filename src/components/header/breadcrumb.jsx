
// libs
import _ from 'lodash';
import React, {PropTypes} from 'react';
import { inflect } from 'fleck';
import { assoc } from 'sprout-data';

// components
import { Link, IndexLink } from '../link/index';

export default class Breadcrumb extends React.Component {

  static contextTypes = {
    location: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
    router: PropTypes.object.isRequired
  };

  constructor(props, context) {
    super(props, context);
    console.log('constructor');
    console.log(context);
  }

  render() {
    const pathname = this.context.location.pathname.replace(/^\/|\/$/gm, '');

    console.log(pathname);
    console.log(this.props.routes);
    console.log(this.props.params);

    const fromRoutes = this.props.routes.map((route) => {
      if (_.isEmpty(route.path)) {
        return null;
      }

      if (route.path === "/" && _.isEmpty(route.name)) {
        return <Link to="home" params={this.props.params}>Home &nbsp;</Link>;
      }

      if (_.isEmpty(route.indexRoute)) {
        return <Link to={route.name} params={this.props.params}>{route.name}&nbsp;</Link>;
      } else {
        return <IndexLink to={route.indexRoute.name} params={this.props.params}>{route.name}&nbsp;</IndexLink>;
      }
    });

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

    return <div className="breadcrumb">{fromRoutes}</div>;
  }
}
