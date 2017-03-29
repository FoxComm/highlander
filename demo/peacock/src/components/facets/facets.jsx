/* flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import styles from './facets.css';

// components
import Checkbox from './kind/checkbox';
import Circle from './kind/circle';
import ColorCircle from './kind/colorcircle';

import type { Facet } from 'types/facets';

type FacetsProps = {
  facets: Array<Facet>,
  whitelist?: Array<string>,
  onSelect?: (facet: string, value: string, selected: boolean) => void,
};

class Facets extends Component {
  props: FacetsProps;

  static defaultProps = {
    facets: [],
    whitelist: [],
  };

  @autobind
  handleClickFacets(facet: string, value: string, selected: boolean): void {
    if (this.props.onSelect) {
      this.props.onSelect(facet, value, selected);
    }
  }

  @autobind
  renderValues(f) {
    let values = {};

    values = _.map(f.values, v => {
      let w = {};
      let key = 'val-' + f.kind + '-' + f.key + '-' + v.label;

      if (f.kind == 'checkbox') {
        w = (<Checkbox
          key={key}
          facet={f.key}
          value={v.value}
          label={v.label}
          click={this.handleClickFacets}
          />
        );
      } else if (f.kind == 'circle') {
        w = (<Circle
          key={key}
          facet={f.key}
          value={v.value}
          label={v.label}
          click={this.handleClickFacets}
          />
        );
      } else if (f.kind == 'color') {
        w = (<ColorCircle
          key={key}
          facet={f.key}
          value={v.value}
          label={v.label}
          click={this.handleClickFacets}
          />
        );
      } else {
        w = (<div key={key}> unsuported type </div>);
      }
      return w;
    });

    return values;
  }

  @autobind
  renderFacet(f) {

    const values = this.renderValues(f);
    const facetStyle = `${f.kind}-facet`;

    return (
      <div key={f.key} styleName="facet">
        <h2>{f.name}</h2>
        <div styleName={facetStyle}>
          {values}
        </div>
      </div>
    );
  }

  render(): Element<*> {
    const { facets, whitelist} = this.props;

    const renderable = _.filter(facets, (f) => {
      return !_.isEmpty(f.values) && (_.isEmpty(whitelist) || _.indexOf(whitelist, f.key) != -1);
    });

    const rendered = _.map(renderable, (f) => {
      return this.renderFacet(f);
    });

    return (
      <div styleName="facets">
        {rendered}
      </div>
    );
  }
}


export default Facets;
