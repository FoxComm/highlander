/* @flow */

// libs
import _ from 'lodash';
import { filter, map, join, flow } from 'lodash/fp';
import React, { Element } from 'react';
import { autobind } from 'core-decorators';

// components
import { Link } from 'react-router';

import styles from './breadcrumbs.css';

import type { RoutesParams, Route } from 'types';

const Delimiter = (props: {idx: number}) => {
  return (
    <li styleName="delimiter" key={`${props.idx}-breadcrumbs-delimeter`}>
      <i className="icon-chevron-right">></i>
    </li>
  );
};

const HomeCrumb = (props: {params: Object}) => {
  return (
    <li styleName="item" key="home-breadcrumbs-link">
      <Link to="/" params={props.params} styleName="link">Home</Link>
    </li>
  );
};

const Crumb = (props: {to: string, params: Object, name: string}) => {
  return (
    <li styleName="item" key={`${props.name}-breadcrumbs-link`}>
      <Link to={props.to} params={props.params} styleName="link">
        {props.name}
      </Link>
    </li>
  );
};

export default class Breadcrumbses extends React.Component {
  props: RoutesParams;

  @autobind
  readableName(route: Route) {
    const parts = route.name.split('-');
    const titlePath = flow(
      filter(item => item !== 'base'),
      map(_.capitalize),
      join(' ')
    )(parts);
    const title = _.get(route, 'title', titlePath);

    if (route.name == 'category') {
      return _.get(this.props, ['params', 'categoryName'], title);
    }

    let titleParam = route.titleParam;
    if (!titleParam && route.path && route.path[0] === ':') {
      titleParam = route.path;
    }

    if (titleParam) {
      return _.get(this.props, ['params', titleParam.slice(1)], title);
    }

    return title;
  }

  delimeter(idx: number) {
    return <Delimiter idx={idx} />;
  }

  get crumbs(): Array<Element<*>> {
    return _.compact(_.map(this.props.routes, (route) => {
      let result = null;

      if (_.isEmpty(route.path)) {
        result = null;
      } else if (route.path === '/' && _.isEmpty(route.name)) {
        result = (
          <HomeCrumb params={this.props.params} />
        );
      } else if (_.isEmpty(route.indexRoute)) {
        result = (
          <Crumb to={route.name} params={this.props.params} name={this.readableName(route)} />
        );
      } else {
        result = (
          <Crumb to={route.indexRoute.name} params={this.props.params} name={this.readableName(route)} />
        );
      }

      return result;
    }));
  }

  render() {
    const fromRoutes = this.crumbs;

    const delimeters = _.range(1, fromRoutes.length).map((idx) => {
      return this.delimeter(idx);
    });

    const withDelimeter = _.zip(fromRoutes, delimeters);

    return (
      <ul styleName="crumbs">
        {withDelimeter}
      </ul>
    );
  }
}
