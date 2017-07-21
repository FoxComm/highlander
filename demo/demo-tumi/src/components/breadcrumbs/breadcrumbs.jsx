/* @flow */

// libs
import _ from 'lodash';
import { filter, map, join, flow } from 'lodash/fp';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { humanize } from 'paragons/categories';

// components
import { Link } from 'react-router';

import styles from './breadcrumbs.css';

import type { Route } from 'types';

const Delimiter = (props: {idx: number}) => {
  return (
    <li styleName="delimiter" key={`${props.idx}-breadcrumbs-delimeter`}>
      /
    </li>
  );
};

const HomeCrumb = () => {
  return (
    <li styleName="item" key="home-breadcrumbs-link">
      <Link to="/" styleName="link">Home</Link>
    </li>
  );
};

const Crumb = (props: {to: string, params: Object, name: string}) => {
  const linkTo = {name: props.to, params: props.params};
  return (
    <li styleName="item" key={`${props.name}-breadcrumbs-link`}>
      <Link to={linkTo} styleName="link">
        {props.name}
      </Link>
    </li>
  );
};

export default class Breadcrumbses extends Component {

  @autobind
  readableName(route: Route) {
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
      return _.get(this.props, ['params', titleParam.slice(1)], title);
    }

    return title;
  }

  delimiter(idx: number) {
    return <Delimiter idx={idx} />;
  }

  get categoryRoutes(): Array<Object> {
    const categoryName = _.get(this.props, ['params', 'categoryName']);
    const subCategory = _.get(this.props, ['params', 'subCategory']);
    const leafCategory = _.get(this.props, ['params', 'leafCategory']);

    const categoryRoutes = [];
    if (categoryName) {
      categoryRoutes.push({
        name: 'category',
        params: { categoryName },
        title: humanize(categoryName, '-'),
      });
    }
    if (subCategory) {
      categoryRoutes.push({
        name: 'category',
        params: { categoryName, subCategory },
        title: humanize(subCategory, '-'),
      });
    }
    if (leafCategory) {
      categoryRoutes.push({
        name: 'category',
        params: { categoryName, subCategory, leafCategory },
        title: humanize(leafCategory, '-'),
      });
    }

    return categoryRoutes;
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
      } else if (_.isEmpty(route.indexRoute) && route.name === 'category') {
        const categoryRoutes = this.categoryRoutes;
        result = _.map(categoryRoutes, part => (
          <Crumb to={part.name} params={part.params} name={this.readableName(part)} />
        ));
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
    const fromRoutes = _.flatten(this.crumbs);

    const delimiters = _.range(1, fromRoutes.length).map((idx) => {
      return this.delimiter(idx);
    });

    const withDelimiter = _.zip(fromRoutes, delimiters);

    return (
      <ul styleName="crumbs" className={this.props.className}>
        {withDelimiter}
      </ul>
    );
  }
}
