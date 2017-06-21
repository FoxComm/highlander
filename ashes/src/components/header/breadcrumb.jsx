// @flow

// libs
import _ from 'lodash';
import { filter, map, join, flow } from 'lodash/fp';
import React, { Component, Element } from 'react';

// components
import { Link, IndexLink } from 'components/link';

// styles
import s from './breadcrumb.css';

const rootPath = process.env.URL_PREFIX;

type Props = {
  routes: Array<any>;
  params: Object;
};

export default class Breadcrumb extends Component {
  props: Props;

  readableName(route: Object): string {
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
    } else {
      return title;
    }
  }

  get crumbs(): Array<Element<*>> {
    const itemCls = `${s.item} icon-chevron-right`;

    return _.compact(this.props.routes.map((route) => {
      if (_.isEmpty(route.path)) {
        return null;
      } else if (route.path === rootPath && _.isEmpty(route.name)) {
        return <Link to="home" key="home" params={this.props.params} className={itemCls}>Home</Link>;
      } else if (_.isEmpty(route.indexRoute)) {
        return (
          <Link to={route.name} key={route.name} params={this.props.params} className={itemCls}>
            {this.readableName(route)}
          </Link>
        );
      }

      const name = route.indexRoute.name;

      return (
        <IndexLink to={name} key={name} params={this.props.params} className={itemCls}>
          {this.readableName(route)}
        </IndexLink>
      );
    }));
  }

  render() {
    return (<div className={s.block}>{this.crumbs}</div>);
  }
}
