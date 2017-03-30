/* flow */

import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import styles from './facets.css';

// components
import Checkbox from './kind/checkbox';
import Circle from './kind/circle';
import ColorCircle from './kind/colorcircle';
import Image from './kind/image';

import type { Facet as TFacet, FacetValue } from 'types/facets';

type FacetsProps = {
  prefix: string,
  facets: Array<TFacet>,
  whitelist?: Array<string>,
  onSelect?: (facet: string, value: string, selected: boolean) => void,
};

class Facets extends Component {
  props: FacetsProps;

  static defaultProps = {
    facets: [],
    whitelist: [],
    prefix: '',
  };

  @autobind
  handleClickFacets(facet: string, value: string, selected: boolean): void {
    if (this.props.onSelect) {
      this.props.onSelect(facet, value, selected);
    }
  }

  @autobind
  renderValues(f: TFacet) {
    return _.map(f.values, (v: FacetValue) => {
      const key = `val-${prefix}-${f.kind}-${f.key}-${v.label}`;
      let w = null;

      if (f.kind == 'checkbox') {
        w = (<Checkbox
          key={key}
          reactKey={key}
          facet={f.key}
          value={v.value}
          label={v.label}
          checked={v.selected}
          click={this.handleClickFacets}
        />);
      } else if (f.kind == 'circle') {
        w = (<Circle
          key={key}
          reactKey={key}
          facet={f.key}
          value={v.value}
          label={v.label}
          checked={v.selected}
          click={this.handleClickFacets}
        />);
      } else if (f.kind == 'color') {
        w = (<ColorCircle
          key={key}
          reactKey={key}
          facet={f.key}
          value={v.value}
          label={v.label}
          checked={v.selected}
          click={this.handleClickFacets}
        />);
      } else if (f.kind == 'image') {
        w = (<Image
          key={key}
          facet={f.key}
          value={v.value}
          label={v.label}
          checked={v.selected}
          click={this.handleClickFacets}
        />);
      }
      return w;
    });
  }

  renderFacet(f: TFacet) {
    const values = this.renderValues(f);
    const facetStyle = `${f.kind}-facet`;

    return (
      <div key={f.key} styleName="facet">
        <div styleName="facet-name">{f.name}</div>
        <div styleName={facetStyle}>
          {values}
        </div>
      </div>
    );
  }

  render(): Element<*> {
    const { facets, whitelist} = this.props;

    // Only show facets that are in the white list if a white list is specified,
    // or that have more than one element.
    // Only showing facets with more then one element was decided because it
    // matches the expected behaviour based on research of other sites.
    const renderable = _.filter(facets, (f) => {
      return f.values.length > 1
        && (_.isEmpty(whitelist) || _.find(whitelist, key => _.toLower(key) == _.toLower(f.key)));
    });

    const rendered = _.map(renderable, (f: TFacet) => {
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
