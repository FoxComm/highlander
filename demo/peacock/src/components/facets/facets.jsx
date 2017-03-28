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
  onSelect: ?Function,
};

class Facets extends Component {
  props: FacetsProps;

  static defaultProps = {
    facets: [],
  };

  @autobind
  handleClickFacets(facet, value, selected): void {
    if (this.props.onSelect) {
      this.props.onSelect(facet, value, selected);
    }
  }

  @autobind
  renderFacet(f) {
    let values = {};

    if (f.kind == 'checkbox') {
      values = _.map(f.values, v => (
        <Checkbox
          facet={f.key}
          value={v.value}
          label={v.label}
          checked={false}
          click={this.handleClickFacets}
        />
      ));
    } else if (f.kind == 'circle') {
      values = _.map(f.values, v => (
        <Circle
          facet={f.key}
          value={v.value}
          label={v.label}
          checked={false}
          click={this.handleClickFacets}
        />
      ));
    } else if (f.kind == 'color') {
      values = _.map(f.values, v => (
        <ColorCircle
          facet={f.key}
          value={v.value}
          label={v.label}
          checked={false}
          click={this.handleClickFacets}
        />
      ));
    } else {
      values = (<div> unsuported type </div>);
    }

    const facetStyle = `${f.kind}-facet`;
    return (
      <div styleName="facet">
        <h2>{f.name}</h2>
        <div styleName={facetStyle}>
          {values}
        </div>
      </div>
    );
  }

  render(): Element<*> {
    const facets = _.map(this.props.facets, (f) => {
      return this.renderFacet(f);
    });

    return (
      <div styleName="facets">
        {facets}
      </div>
    );
  }
}


export default Facets;
