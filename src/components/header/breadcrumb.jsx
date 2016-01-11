
// libs
import _ from 'lodash';
import React, {PropTypes} from 'react';
import { inflect } from 'fleck';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

// components
import { Link, IndexLink } from '../link/index';

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

  @autobind
  readableName(route) {
    const parts = route.name.split('-');
    const composed = _.chain(parts)
      .filter(item => item !== 'base')
      .map(_.capitalize)
      .join(' ')
      .value();

    if (route.path != null && route.path[0] === ':') {
      return _.get(this.props, ['params', route.path.slice(1)], composed);
    } else {
      return composed;
    }
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
        return (
          <li className="fc-breadcrumbs__item" key="home-breadcrumbs-link">
            <Link to="home" params={this.props.params}>Home &nbsp;</Link>
          </li>
        );
      }

      if (_.isEmpty(route.indexRoute)) {
        return (
          <li className="fc-breadcrumbs__item" key={`${route.name}-breadcrumbs-link`}>
            <Link to={route.name} params={this.props.params}>{this.readableName(route)}&nbsp;</Link>
          </li>
        );
      } else {
        return (
          <li className="fc-breadcrumbs__item" key={`${route.name}-breadcrumbs-link`}>
            <IndexLink to={route.indexRoute.name} params={this.props.params}>{this.readableName(route)}&nbsp;</IndexLink>
          </li>
        );
      }
    });

    return (
      <div className="fc-breadcrumbs">
        <ul>
          {fromRoutes}
        </ul>
      </div>
    );
  }
}
