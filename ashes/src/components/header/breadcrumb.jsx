// @flow
// libs
import _ from 'lodash';
import { filter, map, join, flow } from 'lodash/fp';
import React, { PropTypes, Component } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';

// components
import { Link } from '../link/index';

// types
import type { LinkData } from 'modules/breadcrumbs';

const onServer = process.env.ON_SERVER;
const rootPath = onServer ? '/admin' : '/';

type Props = {
  // connected
  routesData: {
    [routeName: string]: Object,
  },
  extraLinks: {
    [routeName: string]: Array<LinkData>,
  },
  routes: Array<Object>,
  params: ?Object,
}

function mapStateToProps(state) {
  return state.breadcrumbs;
}

class Breadcrumb extends Component {
  props: Props;

  @autobind
  readableName(route) {
    const parts = route.name.split('-');
    const titlePath = flow(
      filter(item => item !== 'base'),
      map(_.capitalize),
      join(' ')
    )(parts);
    const title = _.get(route, 'title', titlePath);

    let titleParam = route.titleParam;
    if (!titleParam && route.path && route.path[0] === ':') {
      titleParam = route.path;
    }

    if (titleParam) {
      const paramValue = _.get(this.props, ['params', titleParam.slice(1)]);
      if (paramValue != null) {
        return _.get(this.props.routesData, [route.name, paramValue], paramValue);
      }
    }

    return title;
  }

  delimiter(idx) {
    return (
      <li className="fc-breadcrumbs__delimiter" key={`${idx}-breadcrumbs-delimiter`}>
        <i className="icon-chevron-right" />
      </li>
    );
  }

  get crumbs() {
    return _.flatMap(this.props.routes, route => {
      if (_.isEmpty(route.path) || route.hidden) {
        return [];
      } else if (route.path === rootPath && _.isEmpty(route.name)) {
        return (
          <li className="fc-breadcrumbs__item" key="home-breadcrumbs-link">
            <Link to="home" params={this.props.params} className="fc-breadcrumbs__link">Home</Link>
          </li>
        );
      } else {
        const targetName = route.indexRoute ? route.indexRoute.name : route.name;
        const link = (
          <li className="fc-breadcrumbs__item" key={targetName}>
            <Link
              to={targetName}
              params={this.props.params}
              className="fc-breadcrumbs__link"
              children={this.readableName(route)}
            />
          </li>
        );

        const extraLinks = _.get(this.props.extraLinks, route.name, []).map((linkData: LinkData) => {
          return (
            <li className="fc-breadcrumbs__item" key={linkData.to}>
              <Link
                to={linkData.to}
                params={linkData.params}
                className="fc-breadcrumbs__link"
                children={linkData.title}
              />
            </li>
          );
        });

        return [...extraLinks, link];
      }
    });
  }

  render() {
    const fromRoutes = this.crumbs;

    const delimiters = _.range(1, fromRoutes.length).map((idx) => {
      return this.delimiter(idx);
    });

    const withDelimiter = _.zip(fromRoutes, delimiters);

    return (
      <div className="fc-breadcrumbs">
        <ul>
          {withDelimiter}
        </ul>
      </div>
    );
  }
}

export default connect(mapStateToProps)(Breadcrumb);
